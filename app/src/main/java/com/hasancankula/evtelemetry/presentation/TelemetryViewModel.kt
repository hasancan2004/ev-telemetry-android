package com.hasancankula.evtelemetry.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasancankula.evtelemetry.data.EVTelemetryDto
import com.hasancankula.evtelemetry.data.TelemetryHistoryDto
import com.hasancankula.evtelemetry.data.TelemetrySocketService
import com.hasancankula.evtelemetry.domain.SmartRangeCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// DİKKAT: Success durumuna geçmiş rotamızı çizeceğimiz 'routeHistory' parametresini ekledik!
sealed class TelemetryUiState {
    data object Loading : TelemetryUiState()
    data class Success(
        val telemetry: EVTelemetryDto,
        val estimatedRange: Int,
        val routeHistory: List<TelemetryHistoryDto>
    ) : TelemetryUiState()
    data class Error(val message: String) : TelemetryUiState()
}

class TelemetryViewModel : ViewModel() {

    private val socketService = TelemetrySocketService()
    private val rangeCalculator = SmartRangeCalculator()

    private val _uiState = MutableStateFlow<TelemetryUiState>(TelemetryUiState.Loading)
    val uiState: StateFlow<TelemetryUiState> = _uiState.asStateFlow()

    // Geçmiş rotayı hafızada tutacağımız liste
    private var currentRouteHistory = emptyList<TelemetryHistoryDto>()

    init {
        // init bloğunda direkt bu yeni akışı başlatıyoruz
        fetchHistoryAndStartStream()
    }

    private fun fetchHistoryAndStartStream() {
        viewModelScope.launch {
            // 1. Önce REST API üzerinden veritabanındaki son 100 konumu çekiyoruz
            val history = socketService.getTelemetryHistory()

            // Backend en yeni veriyi en üstte veriyor. Çizginin eskiden yeniye akması için tersine çeviriyoruz
            currentRouteHistory = history.reversed()

            // 2. Geçmişi başarıyla aldıktan sonra WebSocket şelalesini (canlı veriyi) dinlemeye başlıyoruz
            socketService.getTelemetryStream()
                .catch { exception ->
                    _uiState.value = TelemetryUiState.Error(exception.message ?: "Bağlantı koptu.")
                }
                .collect { telemetryData ->
                    val dynamicRange = rangeCalculator.calculateDynamicRange(telemetryData)

                    // 3. Araba hareket ettikçe, o anki konumu rotanın sonuna ekliyoruz ki mavi çizgimiz de aracı takip etsin
                    val currentPoint = TelemetryHistoryDto(
                        latitude = telemetryData.latitude,
                        longitude = telemetryData.longitude,
                        speedKmh = telemetryData.speedKmh
                    )
                    currentRouteHistory = currentRouteHistory + currentPoint

                    // Tüm veriyi UI'a fırlatıyoruz
                    _uiState.value = TelemetryUiState.Success(
                        telemetry = telemetryData,
                        estimatedRange = dynamicRange,
                        routeHistory = currentRouteHistory
                    )
                }
        }
    }

    fun setSuspensionMode(mode: String) {
        viewModelScope.launch {
            val commandJson = """{"suspension_mode": "$mode"}"""
            socketService.sendCommand(commandJson)
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketService.closeClient()
    }
}