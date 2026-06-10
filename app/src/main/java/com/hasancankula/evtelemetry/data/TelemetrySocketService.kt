package com.hasancankula.evtelemetry.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

class TelemetrySocketService {

    // Railway ile bağlantı için SSL (WSS/HTTPS) destekli client
    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20_000
        }
    }

    private val jsonFormatter = Json { ignoreUnknownKeys = true }
    private var activeSession: DefaultClientWebSocketSession? = null

    // Bulut Adresimiz
    private val BASE_URL = "https://ev-telemetry-backend-production.up.railway.app"
    private val WS_URL = "wss://ev-telemetry-backend-production.up.railway.app/ws/telemetry"

    fun getTelemetryStream(): Flow<List<EVTelemetryDto>> = flow {
        try {
            // Portsuz, wss üzerinden doğrudan bağlantı
            val session = client.webSocketSession(WS_URL)
            activeSession = session

            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val jsonText = frame.readText()
                    val fleetData = jsonFormatter.decodeFromString<List<EVTelemetryDto>>(jsonText)
                    emit(fleetData)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            activeSession = null
        }
    }

    suspend fun sendCommand(commandJson: String) {
        try {
            activeSession?.send(Frame.Text(commandJson))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getTelemetryHistory(vehicleId: String): List<TelemetryHistoryDto> {
        return try {
            val response = client.get("$BASE_URL/api/v1/telemetry/history?vehicle_id=$vehicleId&limit=100")
            val jsonText = response.bodyAsText()
            jsonFormatter.decodeFromString<List<TelemetryHistoryDto>>(jsonText)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getChargingStations(): List<ChargingStationDto> {
        return try {
            val response = client.get("$BASE_URL/api/v1/charging-stations")
            val jsonText = response.bodyAsText()
            jsonFormatter.decodeFromString<List<ChargingStationDto>>(jsonText)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun closeClient() {
        client.close()
    }
}