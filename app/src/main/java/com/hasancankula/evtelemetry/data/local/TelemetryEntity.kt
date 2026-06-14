package com.hasancankula.evtelemetry.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// EKSİK OLAN VE HATAYA SEBEP OLAN KRİTİK SATIR EKLENDİ
@Entity(tableName = "telemetry_logs")
data class TelemetryEntity(
    @PrimaryKey
    val vehicleId: String,
    val vehicleModel: String,
    val speedKmh: Int,
    val batteryLevelPct: Float,
    val latitude: Double,
    val longitude: Double,
    val maintenanceRiskPct: Float,
    val ecoScore: Int,
    val timestamp: Long = System.currentTimeMillis()
)