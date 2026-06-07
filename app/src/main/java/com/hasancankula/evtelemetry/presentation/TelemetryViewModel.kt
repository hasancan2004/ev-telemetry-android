package com.hasancankula.evtelemetry.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasancankula.evtelemetry.data.EVTelemetryDto
import com.hasancankula.evtelemetry.data.TelemetrySocketService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// Arayüzümüzün (UI) içinde bulunabileceği farklı durumları (State) temsil eder.
sealed class TelemetryUiState {
    data object Loading : TelemetryUiState()
    data class Success(val telemetry: EVTelemetryDto) : TelemetryUiState()
    data class Error(val message: String) : TelemetryUiState()
}

class TelemetryViewModel : ViewModel() {

    private val socketService = TelemetrySocketService()

    // Sadece bu ViewModel'in güncelleyebileceği, dışarıya (ekrana) kapalı State
    private val _uiState = MutableStateFlow<TelemetryUiState>(TelemetryUiState.Loading)

    // Compose UI'ın (ekranın) abone olup dinleyeceği, değiştirilemez State
    val uiState: StateFlow<TelemetryUiState> = _uiState.asStateFlow()

    init {
        // ViewModel hafızada oluşturulduğu an dinlemeyi başlat
        startTelemetryStream()
    }

    private fun startTelemetryStream() {
        viewModelScope.launch {
            socketService.getTelemetryStream()
                .catch { exception ->
                    _uiState.value = TelemetryUiState.Error(exception.message ?: "Bağlantı koptu.")
                }
                .collect { telemetryData ->
                    _uiState.value = TelemetryUiState.Success(telemetryData)
                }
        }
    }

    // ====================================================================
    // YENİ EKLENEN KISIM: Arayüzden gelen komutu araca gönderen köprü
    // ====================================================================
    fun setSuspensionMode(mode: String) {
        // Asenkron (Coroutine) bir işlem olduğu için viewModelScope içinde başlatıyoruz
        viewModelScope.launch {
            // Python FastAPI'nin beklediği tam JSON formatını String olarak hazırlıyoruz
            val commandJson = """{"suspension_mode": "$mode"}"""

            // Hazırladığımız bu JSON mermisini Data katmanındaki tünele ateşliyoruz
            socketService.sendCommand(commandJson)
        }
    }

    // Uygulama tamamen kapandığında bellek sızıntısını önler
    override fun onCleared() {
        super.onCleared()
        socketService.closeClient()
    }
}