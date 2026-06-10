package com.hasancankula.evtelemetry.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasancankula.evtelemetry.data.ChargingStationDto
import com.hasancankula.evtelemetry.data.EVTelemetryDto
import com.hasancankula.evtelemetry.data.SettingsDataStore
import com.hasancankula.evtelemetry.data.TelemetryHistoryDto
import com.hasancankula.evtelemetry.data.TelemetrySocketService
import com.hasancankula.evtelemetry.domain.SmartRangeCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class FleetUiState {
    data object Loading : FleetUiState()
    data class Success(val vehicles: List<EVTelemetryDto>) : FleetUiState()
    data class Error(val message: String) : FleetUiState()
}

// Detay ekranının ihtiyacı olan tüm dinamik veriler
data class VehicleDetailState(
    val telemetry: EVTelemetryDto? = null,
    val estimatedRange: Int = 0,
    val routeHistory: List<TelemetryHistoryDto> = emptyList()
)

// 1. Sınıf tanımını AndroidViewModel yapıyoruz ki 'application' (Context) alabilsin
class TelemetryViewModel(application: Application) : AndroidViewModel(application) {

    // İstasyon listesini tutacak StateFlow
    private val _chargingStations = MutableStateFlow<List<ChargingStationDto>>(emptyList())
    val chargingStations: StateFlow<List<ChargingStationDto>> = _chargingStations.asStateFlow()
    private val settingsDataStore = SettingsDataStore(application)
    private val socketService = TelemetrySocketService()
    private val rangeCalculator = SmartRangeCalculator()

    // ==========================================
    // DİNAMİK AYARLAR (DATASTORE BAĞLANTILI)
    // ==========================================

    // DataStore'dan gelen canlı veriyi doğrudan StateFlow'a çeviriyoruz
    val aiAlarmThreshold: StateFlow<Float> = settingsDataStore.aiThresholdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 75f)

    val geofenceRadiusKm: StateFlow<Float> = settingsDataStore.geofenceRadiusFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20f)

    // Kaydırıcı (Slider) değiştikçe DataStore'a kaydet
    fun updateAiThreshold(newValue: Float) {
        viewModelScope.launch { settingsDataStore.saveAiThreshold(newValue) }
    }

    fun updateGeofenceRadius(newValue: Float) {
        viewModelScope.launch { settingsDataStore.saveGeofenceRadius(newValue) }
    }

    // ==========================================
    // FİLO VE ARAÇ DURUMU (UI STATE)
    // ==========================================

    private val _uiState = MutableStateFlow<FleetUiState>(FleetUiState.Loading)
    val uiState: StateFlow<FleetUiState> = _uiState.asStateFlow()

    // Detay ekranı için izole bir state akışı
    private val _detailState = MutableStateFlow(VehicleDetailState())
    val detailState: StateFlow<VehicleDetailState> = _detailState.asStateFlow()

    private var currentRouteHistory = emptyList<TelemetryHistoryDto>()
    private var selectedVehicleId: String? = null

    init {
        startFleetStream()
        loadChargingStations()
    }
    private fun loadChargingStations() {
        viewModelScope.launch {
            val stations = socketService.getChargingStations()
            _chargingStations.value = stations
        }
    }

    private fun startFleetStream() {
        viewModelScope.launch {
            socketService.getTelemetryStream()
                .catch { exception ->
                    _uiState.value = FleetUiState.Error(exception.message ?: "Bağlantı koptu.")
                }
                .collect { fleetList ->
                    _uiState.value = FleetUiState.Success(fleetList)

                    // KRİTİK KISIM: Eğer kullanıcı bir aracın detayındaysa
                    selectedVehicleId?.let { id ->
                        val activeVehicle = fleetList.find { it.vehicleId == id }
                        activeVehicle?.let { vehicle ->
                            val dynamicRange = rangeCalculator.calculateDynamicRange(vehicle)

                            // Canlı gelen koordinatı harita çizgi listemize ekliyoruz
                            val currentPoint = TelemetryHistoryDto(
                                vehicleId = vehicle.vehicleId,
                                latitude = vehicle.latitude,
                                longitude = vehicle.longitude,
                                speedKmh = vehicle.speedKmh
                            )
                            if (currentRouteHistory.isEmpty() || currentRouteHistory.last().latitude != vehicle.latitude) {
                                currentRouteHistory = currentRouteHistory + currentPoint
                            }

                            _detailState.value = VehicleDetailState(
                                telemetry = vehicle,
                                estimatedRange = dynamicRange,
                                routeHistory = currentRouteHistory
                            )
                        }
                    }
                }
        }
    }

    // Kullanıcı listeden bir araca tıkladığında bu fonksiyon çalışacak
    fun selectVehicle(vehicleId: String) {
        selectedVehicleId = vehicleId
        _detailState.value = VehicleDetailState() // Yeni veri gelene kadar ekranı temizle

        viewModelScope.launch {
            // REST API'den sadece bu tıklanan aracın geçmişini çekiyoruz
            val history = socketService.getTelemetryHistory(vehicleId)
            currentRouteHistory = history.reversed()
        }
    }

    // Detay ekranından çıkınca hafızayı temizleme fonksiyonu
    fun clearSelectedVehicle() {
        selectedVehicleId = null
        currentRouteHistory = emptyList()
        _detailState.value = VehicleDetailState()
    }

    // Komut gönderirken artık hangi araca gönderdiğimizi de Python'a söylüyoruz
    fun setSuspensionMode(vehicleId: String, mode: String) {
        viewModelScope.launch {
            val commandJson = """{"vehicle_id": "$vehicleId", "suspension_mode": "$mode"}"""
            socketService.sendCommand(commandJson)
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketService.closeClient()
    }
}