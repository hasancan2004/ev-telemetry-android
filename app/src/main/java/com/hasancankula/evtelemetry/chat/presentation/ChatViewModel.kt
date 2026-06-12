package com.hasancankula.evtelemetry.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hasancankula.evtelemetry.chat.data.ChatService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)
class ChatViewModel : ViewModel() {

    private val chatService = ChatService()

    // Ekran açıldığında asistanın ilk karşılama mesajı
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("Merhaba! Ben Filo Yapay Zeka Asistanıyım. Bana 'Hangi araçların şarja ihtiyacı var?' veya 'Filonun durumu ne?' gibi sorular sorabilirsiniz.", false))

    )

    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading : StateFlow<Boolean> = _isLoading.asStateFlow()

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // 1. Kullanıcının mesajını ekrana ekle
        _messages.value = _messages.value + ChatMessage(userMessage, true)
        _isLoading.value = true

        // 2. Arka planda buluta istek at ve cevabı bekle
         viewModelScope.launch {
             val reply = chatService.sendMessage(userMessage)
             // 3. Gelen cevabı ekrana ekle ve yükleniyor ikonunu kapat
             _messages.value = _messages.value + ChatMessage(reply, false)
             _isLoading.value = false
         }
    }
}