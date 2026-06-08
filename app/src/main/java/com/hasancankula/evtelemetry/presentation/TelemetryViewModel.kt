package com.hasancankula.evtelemetry.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasancankula.evtelemetry.data.EVTelemetryDto
import com.hasancankula.evtelemetry.data.TelemetrySocketService
import com.hasancankula.evtelemetry.domain.SmartRangeCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// DİKKAT: Success durumuna 'estimatedRange' adında yeni bir parametre ekledik!
sealed class TelemetryUiState {
    data object Loading : TelemetryUiState()
    data class Success(val telemetry: EVTelemetryDto, val estimatedRange: Int) : TelemetryUiState()
    data class Error(val message: String) : TelemetryUiState()
}

class TelemetryViewModel : ViewModel() {

    private val socketService = TelemetrySocketService()

    // YENİ: Domain katmanındaki zeki hesaplayıcımızı ViewModel'e çağırıyoruz
    private val rangeCalculator = SmartRangeCalculator()

    private val _uiState = MutableStateFlow<TelemetryUiState>(TelemetryUiState.Loading)
    val uiState: StateFlow<TelemetryUiState> = _uiState.asStateFlow()

    init {
        startTelemetryStream()
    }

    private fun startTelemetryStream() {
        viewModelScope.launch {
            socketService.getTelemetryStream()
                .catch { exception ->
                    _uiState.value = TelemetryUiState.Error(exception.message ?: "Bağlantı koptu.")
                }
                .collect { telemetryData ->
                    // Şelaleden yeni JSON geldiği anda o anki şartlara göre menzili hesaplıyoruz
                    val dynamicRange = rangeCalculator.calculateDynamicRange(telemetryData)

                    // Hesaplanan bu menzili de UI'a (Ekrana) fırlatıyoruz
                    _uiState.value = TelemetryUiState.Success(telemetryData, dynamicRange)
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