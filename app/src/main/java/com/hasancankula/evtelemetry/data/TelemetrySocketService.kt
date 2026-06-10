package com.hasancankula.evtelemetry.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
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

    private var activeSession: DefaultClientWebSocketSession? = null

    fun getTelemetryStream(): Flow<List<EVTelemetryDto>> = flow {
        try {
            val session = client.webSocketSession(
                method = HttpMethod.Get,
                host = "10.0.2.2", 
                port = 8000,
                path = "/ws/telemetry"
            )
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
            // YENİ TELEFON IP'MİZ
            val response = client.get("http://10.230.108.179:8000/api/v1/telemetry/history?vehicle_id=$vehicleId&limit=100")
            val jsonText = response.bodyAsText()
            jsonFormatter.decodeFromString<List<TelemetryHistoryDto>>(jsonText)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Sunucudan aktif şarj istasyonlarının listesini çeken fonksiyon
    suspend fun getChargingStations(): List<ChargingStationDto> {
        return try {
            val response = client.get("http://10.230.108.179:8000/api/v1/charging-stations")
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