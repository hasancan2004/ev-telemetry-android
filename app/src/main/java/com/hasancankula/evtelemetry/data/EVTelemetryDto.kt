package com.hasancankula.evtelemetry.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EVTelemetryDto(
    // ==========================================
    // YENİ: Aracın Kimliği (Plakası)
    // ==========================================
    @SerialName("vehicle_id")
    val vehicleId: String,

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
    val tirePressurePsi: Double,

    @SerialName("latitude")
    val latitude: Double = 0.0,

    @SerialName("longitude")
    val longitude: Double = 0.0,

    @SerialName("maintenance_risk_pct")
    val maintenanceRiskPct: Double = 0.0



)