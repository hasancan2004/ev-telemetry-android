package com.hasancankula.evtelemetry.presentation

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hasancankula.evtelemetry.R
import com.hasancankula.evtelemetry.data.AnalyticsKpiDto
import com.hasancankula.evtelemetry.data.ChargingStationDto
import com.hasancankula.evtelemetry.data.EVTelemetryDto
import com.hasancankula.evtelemetry.data.SettingsDataStore
import com.hasancankula.evtelemetry.data.TelemetryHistoryDto
import com.hasancankula.evtelemetry.data.TelemetrySocketService
import com.hasancankula.evtelemetry.domain.SmartRangeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@HiltViewModel
class TelemetryViewModel @Inject constructor(
    application: Application,
    private val socketService: TelemetrySocketService

) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val _chargingStations = MutableStateFlow<List<ChargingStationDto>>(emptyList())
    val chargingStations: StateFlow<List<ChargingStationDto>> = _chargingStations.asStateFlow()
    private val settingsDataStore = SettingsDataStore(application)

    private val rangeCalculator = SmartRangeCalculator()

    private val notifiedRiskVehicles = mutableSetOf<String>()
    private val notifiedGeofenceVehicles = mutableSetOf<String>()

    // YENİ: Analiz verilerini tutacak StateFlow
    private val _analyticsData = MutableStateFlow<AnalyticsKpiDto?>(null)
    val analyticsData: StateFlow<AnalyticsKpiDto?> = _analyticsData.asStateFlow()

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

    private val _uiState = MutableStateFlow<FleetUiState>(FleetUiState.Loading)
    val uiState: StateFlow<FleetUiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow(VehicleDetailState())
    val detailState: StateFlow<VehicleDetailState> = _detailState.asStateFlow()

    private var currentRouteHistory = emptyList<TelemetryHistoryDto>()
    private var selectedVehicleId: String? = null

    init {
        startFleetStream()
        loadChargingStations()
        fetchAnalytics() // Başlangıçta analizleri de yükle
    }

    private fun loadChargingStations() {
        viewModelScope.launch {
            val stations = socketService.getChargingStations()
            _chargingStations.value = stations
        }
    }

    // YENİ: Python'dan Analizleri Çek
    fun fetchAnalytics() {
        viewModelScope.launch {
            val data = socketService.getAnalytics()
            _analyticsData.value = data
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
                    checkAndTriggerNotifications(fleetList)

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

    private fun checkAndTriggerNotifications(fleetList: List<EVTelemetryDto>) {
        val currentThreshold = aiAlarmThreshold.value
        fleetList.forEach { vehicle ->
            if (vehicle.maintenanceRiskPct >= currentThreshold) {
                if (!notifiedRiskVehicles.contains(vehicle.vehicleId)) {
                    sendPushNotification(
                        title = "🚨 KRİTİK: Yüksek Arıza Riski!",
                        message = "${vehicle.vehicleModel} (${vehicle.vehicleId}) aracında %${vehicle.maintenanceRiskPct} arıza riski tespit edildi. Acil bakım önerilir.",
                        notificationId = vehicle.vehicleId.hashCode() + 1
                    )
                    notifiedRiskVehicles.add(vehicle.vehicleId)
                }
            } else {
                notifiedRiskVehicles.remove(vehicle.vehicleId)
            }

            if (vehicle.geofenceBreach) {
                if (!notifiedGeofenceVehicles.contains(vehicle.vehicleId)) {
                    sendPushNotification(
                        title = "📍 İHLAL: Sınır Dışı Araç!",
                        message = "${vehicle.vehicleModel} (${vehicle.vehicleId}) aracı güvenli bölge sınırlarını aştı!",
                        notificationId = vehicle.vehicleId.hashCode() + 2
                    )
                    notifiedGeofenceVehicles.add(vehicle.vehicleId)
                }
            } else {
                notifiedGeofenceVehicles.remove(vehicle.vehicleId)
            }
        }
    }

    private fun sendPushNotification(title: String, message: String, notificationId: Int) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "telemetry_alerts_channel"
        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()
        notificationManager.notify(notificationId, notification)
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

    fun setSuspensionMode(vehicleId: String, mode: String) {
        viewModelScope.launch {
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

    fun reserveStation(stationId: String) {
        viewModelScope.launch {
            val success = socketService.reserveChargingStation(stationId)
            if (success) {
                loadChargingStations()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketService.closeClient()
    }
}