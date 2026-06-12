package com.hasancankula.evtelemetry.data

import kotlinx.serialization.Serializable

@Serializable
data class AnalyticsResponseDto(
    val kpi: AnalyticsKpiDto,
)

@Serializable
data class AnalyticsKpiDto(
    val total_energy_kwh: Double,
    val avg_eco_score: Int,
    val critical_risk_count: Int
)
