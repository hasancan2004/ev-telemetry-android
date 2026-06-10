package com.hasancankula.evtelemetry.data

import kotlinx.serialization.Serializable

@Serializable
data class ChargingStationDto(
    val id: String,
    val name: String,
    val provider: String,
    val latitude: Double,
    val longitude: Double,
    val is_available: Boolean
)