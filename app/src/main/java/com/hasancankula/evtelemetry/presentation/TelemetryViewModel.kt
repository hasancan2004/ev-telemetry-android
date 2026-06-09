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

// YENİ: Artık tek bir araba değil, tüm filonun listesini tutuyoruz!
sealed class FleetUiState {
    data object Loading : FleetUiState()
    data class Success(val vehicles: List<EVTelemetryDto>) : FleetUiState()
    data class Error(val message: String) : FleetUiState()
}

class TelemetryViewModel : ViewModel() {

    private val socketService = TelemetrySocketService()

    // UiState tipimizi yeni FleetUiState ile değiştirdik
    private val _uiState = MutableStateFlow<FleetUiState>(FleetUiState.Loading)
    val uiState: StateFlow<FleetUiState> = _uiState.asStateFlow()

    init {
        startFleetStream()
    }

    private fun startFleetStream() {
        viewModelScope.launch {
            // Soketten gelen liste akışını dinlemeye başlıyoruz
            socketService.getTelemetryStream()
                .catch { exception ->
                    _uiState.value = FleetUiState.Error(exception.message ?: "Bağlantı koptu.")
                }
                .collect { fleetList ->
                    // Gelen 3 araçlık listeyi anında UI'a (Ekrana) fırlatıyoruz
                    _uiState.value = FleetUiState.Success(fleetList)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketService.closeClient()
    }
}