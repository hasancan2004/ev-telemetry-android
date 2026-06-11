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

data class VehicleDetailState(
    val telemetry: EVTelemetryDto? = null,
    val estimatedRange: Int = 0,
    val routeHistory: List<TelemetryHistoryDto> = emptyList()
)

class TelemetryViewModel(application: Application) : AndroidViewModel(application) {

    private val _chargingStations = MutableStateFlow<List<ChargingStationDto>>(emptyList())
    val chargingStations: StateFlow<List<ChargingStationDto>> = _chargingStations.asStateFlow()
    private val settingsDataStore = SettingsDataStore(application)
    private val socketService = TelemetrySocketService()
    private val rangeCalculator = SmartRangeCalculator()

    // ==========================================
    // DİNAMİK AYARLAR (DATASTORE BAĞLANTILI)
    // ==========================================

    val aiAlarmThreshold: StateFlow<Float> = settingsDataStore.aiThresholdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 75f)

    val geofenceRadiusKm: StateFlow<Float> = settingsDataStore.geofenceRadiusFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20f)

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

                    selectedVehicleId?.let { id ->
                        val activeVehicle = fleetList.find { it.vehicleId == id }
                        activeVehicle?.let { vehicle ->
                            val dynamicRange = rangeCalculator.calculateDynamicRange(vehicle)

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

    fun selectVehicle(vehicleId: String) {
        selectedVehicleId = vehicleId
        _detailState.value = VehicleDetailState()

        viewModelScope.launch {
            val history = socketService.getTelemetryHistory(vehicleId)
            currentRouteHistory = history.reversed()
        }
    }

    fun clearSelectedVehicle() {
        selectedVehicleId = null
        currentRouteHistory = emptyList()
        _detailState.value = VehicleDetailState()
    }

    // ==========================================
    // ÇİFT YÖNLÜ İLETİŞİM (REMOTE COMMANDS)
    // ==========================================

    // GÜNCELLENDİ: Python backend'in beklediği tam JSON formatı oluşturuldu.
    fun setSuspensionMode(vehicleId: String, mode: String) {
        viewModelScope.launch {
            // Python'daki `data.get("action")` ve `data.get("value")` ile eşleşmeli
            val commandJson = """
                {
                    "action": "set_suspension",
                    "vehicle_id": "$vehicleId",
                    "value": "$mode"
                }
            """.trimIndent()

            socketService.sendCommand(commandJson)
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketService.closeClient()
    }
}