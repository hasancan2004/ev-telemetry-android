package com.hasancankula.evtelemetry.chat.data

import androidx.compose.ui.autofill.ContentType
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class ChatRequestDto(val message: String)

@Serializable
data class ChatResponseDto(val reply: String)
class ChatService {
    private val client = HttpClient(CIO)
    private val jsonFormatter = Json { ignoreUnknownKeys = true }
    private val BASE_URL = "https://ev-telemetry-backend-production.up.railway.app"

    suspend fun sendMessage(message: String) : String {
        return try {
            val requestObj = ChatRequestDto(message)
            val jsonBody = jsonFormatter.encodeToString(ChatRequestDto.serializer(), requestObj)

            println("🤖 ASİSTANA SORULUYOR: $message")
            val response = client.post("$BASE_URL/api/v1/chat") {
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(jsonBody)
            }

            val responseText = response.bodyAsText()
            val responseObj = jsonFormatter.decodeFromString<ChatResponseDto>(responseText)
            println("✅ ASİSTAN CEVAPLADI: ${responseObj.reply}")
            responseObj.reply
        } catch (e: Exception) {
            e.printStackTrace()
            "Bağlantı hatası: Asistana şu an ulaşılamıyor. Lütfen internet bağlantınızı kontrol edin."
        }
    }
}