package com.hasancankula.evtelemetry.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class TelemetrySocketService {

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    private val jsonFormatter = Json { ignoreUnknownKeys = true }

    // Çift yönlü iletişim için aktif tünelimizi (oturum) hafızada tutuyoruz
    private var activeSession: DefaultClientWebSocketSession? = null

    // 1. YÖN: Araçtan gelen veriyi dinleyen şelalemiz (Aynı kalıyor, sadece session yapısı değişti)
    fun getTelemetryStream(): Flow<EVTelemetryDto> = flow {
        try {
            // Tüneli açıp aktif oturum değişkenimize kaydediyoruz
            val session = client.webSocketSession(
                method = HttpMethod.Get,
                host = "10.0.2.2",
                port = 8000,
                path = "/ws/telemetry"
            )
            activeSession = session

            // incoming.receive() yerine doğrudan kanalın içinde dönerek gelen verileri yakalıyoruz
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val jsonText = frame.readText()
                    val telemetryDto = jsonFormatter.decodeFromString<EVTelemetryDto>(jsonText)
                    emit(telemetryDto)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Flow iptal olursa veya bağlantı koparsa tüneli boşa çıkarıyoruz
            activeSession = null
        }
    }

    // 2. YÖN: Bizden araca komut gönderen yepyeni fonksiyonumuz
    suspend fun sendCommand(commandJson: String) {
        try {
            // Eğer aktif bir tünel (session) varsa, içine JSON komutumuzu fırlat
            activeSession?.send(Frame.Text(commandJson))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun closeClient() {
        client.close()
    }
}