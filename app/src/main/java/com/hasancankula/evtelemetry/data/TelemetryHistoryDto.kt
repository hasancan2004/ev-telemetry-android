package com.hasancankula.evtelemetry.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TelemetryHistoryDto(
    @SerialName("id")
    val id: Int = 0,

    @SerialName("latitude")
    val latitude: Double,

    @SerialName("longitude")
    val longitude: Double,

    @SerialName("speed_kmh")
    val speedKmh: Int
)
