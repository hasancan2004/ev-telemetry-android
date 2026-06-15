package com.hasancankula.evtelemetry.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasancankula.evtelemetry.chat.data.GeminiService
import com.hasancankula.evtelemetry.data.local.TelemetryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(val text: String, val isUser: Boolean)

// YENİ: Hilt ile gerçek servisleri içeri alıyoruz
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val geminiService: GeminiService,
    private val telemetryDao: TelemetryDao
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("Merhaba! Ben Gemini tabanlı Filo Asistanıyım. Bana araçların durumu hakkında sorular sorabilirsiniz.", false))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading : StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        _messages.value = _messages.value + ChatMessage(userMessage, true)
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // 1. Veritabanından (Room) araçların son durumunu çekiyoruz
                var fleetContextText = "Filoda kayıtlı araç verisi bulunamadı."
                telemetryDao.getAllTelemetriesFlow().collect { localData ->
                    if (localData.isNotEmpty()) {
                        val stringBuilder = java.lang.StringBuilder()
                        localData.forEach {
                            stringBuilder.append("- Araç ${it.vehicleId} (${it.vehicleModel}): Hız ${it.speedKmh} km/s, Batarya %${it.batteryLevelPct}, Arıza Riski %${it.maintenanceRiskPct}\n")
                        }
                        fleetContextText = stringBuilder.toString()
                    }

                    // 2. Kullanıcı mesajı ve filo durumuyla Gemini'ye soruyoruz
                    val reply = geminiService.getAiResponse(userMessage, fleetContextText)

                    _messages.value = _messages.value + ChatMessage(reply, false)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage("Bağlantı hatası: ${e.localizedMessage}", false)
                _isLoading.value = false
            }
        }
    }
}