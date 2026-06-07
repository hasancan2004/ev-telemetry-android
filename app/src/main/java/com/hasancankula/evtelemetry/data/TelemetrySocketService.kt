package com.hasancankula.evtelemetry.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class TelemetrySocketService {

    // Ktor HTTP İstemcimizi WebSocket destekli CIO motoruyla kuruyoruz
    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    // JSON formatındaki veriyi Kotlin nesnelerine çevirecek olan araç
    // ignoreUnknownKeys = true: Backend'den fazladan veri gelirse çökmeyi önler
    private val jsonFormatter = Json { ignoreUnknownKeys = true }

    // Dışarıya sürekli bir veri "Akışı" (Flow) sunan ana fonksiyonumuz
    fun getTelemetryStream(): Flow<EVTelemetryDto> = flow {
        try {
            /* * KRİTİK BİLGİ:
             * Android Emülatörü, bilgisayarının "localhost"una (127.0.0.1) doğrudan erişemez.
             * Emülatörün bilgisayarını görmesi için her zaman "10.0.2.2" IP adresi kullanılır.
             * (Eğer uygulamayı fiziksel bir telefonda test edeceksen, buraya bilgisayarının Wi-Fi IPv4 adresini yazmalısın.)
             */
            client.webSocket(
                method = HttpMethod.Get,
                host = "10.0.2.2",
                port = 8000,
                path = "/ws/telemetry"
            ) {
                // Bağlantı açık kaldığı sürece sonsuz döngüde veriyi dinle
                while (true) {
                    val incomingData = incoming.receive()

                    // Gelen veri metin (JSON) formatındaysa işle
                    if (incomingData is Frame.Text) {
                        val jsonText = incomingData.readText()

                        // JSON string'ini bizim DTO modelimize dönüştür (Deserialize)
                        val telemetryDto = jsonFormatter.decodeFromString<EVTelemetryDto>(jsonText)

                        // Flow'a yeni veriyi fırlat (Emit)
                        emit(telemetryDto)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Gerçek bir projede buraya bağlantı kopması durumunda UI'a hata fırlatacak bir yapı (Result wrapper vb.) eklenir.
        }
    }

    // Uygulama kapanırken veya arka plana atılırken soketi kapatmak için
    fun closeClient() {
        client.close()
    }
}