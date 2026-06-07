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
// İleride ekranda if(durum == Success) ise arabayı çiz, if(durum == Loading) ise dönen tekerlek göster diyeceğiz.
sealed class TelemetryUiState {
    data object Loading : TelemetryUiState()
    data class Success(val telemetry: EVTelemetryDto) : TelemetryUiState()
    data class Error(val message: String) : TelemetryUiState()
}
class TelemetryViewModel : ViewModel() {
    // Normalde büyük projelerde burada Dependency Injection (Hilt gibi araçlar) kullanılır,
    // ancak biz karmaşıklığı azaltmak için servisimizi doğrudan burada oluşturuyoruz.
    private val socketService = TelemetrySocketService()

    // Sadece bu ViewModel'in güncelleyebileceği, dışarıya (ekrana) kapalı State (Durum) havuzu
    private val _uiState = MutableStateFlow<TelemetryUiState>(TelemetryUiState.Loading)

    // Compose UI'ın (ekranın) abone olup dinleyeceği, değiştirilemez (sadece okunur) State
    val uiState: StateFlow<TelemetryUiState> = _uiState.asStateFlow()

    init {
        // ViewModel hafızada oluşturulduğu an, arka plandaki soket dinleme işini başlat
        startTelemetryStream()
    }

    private fun startTelemetryStream() {
        // viewModelScope: Ekran kapatılıp ViewModel öldüğünde arka plandaki asenkron işlemlerin
        // de otomatik olarak iptal edilmesini sağlar.
        viewModelScope.launch {
            socketService.getTelemetryStream()
                .catch { exception ->
                    // Eğer bağlantı koparsa veya arkadaki Python sunucusu aniden kapanırsa burası çalışır
                    _uiState.value = TelemetryUiState.Error(exception.message ?: "Bağlantı Koptu")
                }
                .collect { telemetryData ->
                    // Soketten her saniye yeni bir JSON verisi geldiğinde UI State'i (Ekran Durumunu) günceller.
                    // Compose ekranı bu state'i dinlediği için kendini otomatik olarak yeniden çizer.
                    _uiState.value = TelemetryUiState.Success(telemetryData)
                }
        }
    }

    // Uygulama tamamen kapandığında veya ekran yok edildiğinde Android tarafından otomatik çağrılır
    override fun onCleared() {
        super.onCleared()
        // Temizlik: Memory Leak (bellek sızıntısı) olmaması için WebSocket bağlantısını güvenlice kapatıyoruz.
        socketService.closeClient()
    }
}