package com.hasancankula.evtelemetry.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EVTelemetryDto(
    @SerialName("speed_kmh")
    val speedKmh: Int,

    @SerialName("battery_level_pct")
    val batteryLevelPct: Double,

    @SerialName("regeneration_kw")
    val regenerationKw: Double,

    @SerialName("cabin_temperature_c")
    val cabinTemperatureC: Double,

    @SerialName("suspension_mode")
    val suspensionMode: String,

    @SerialName("tire_pressure_psi")
    val tirePressurePsi: Double
)
