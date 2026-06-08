package com.hasancankula.evtelemetry.domain

import com.hasancankula.evtelemetry.data.EVTelemetryDto
import kotlin.math.abs

class SmartRangeCalculator {

    // Sabit değerler (Örnek bir elektrikli aracın fabrika verileri)
    private val maxRangeKm = 500.0 // %100 batarya ile ideal şartlardaki maksimum menzil
    private val optimalTemp = 22.0 // Klimanın en az enerji harcadığı ideal kabin sıcaklığı

    fun calculateDynamicRange(telemetry: EVTelemetryDto): Int {
        // 1. Temel Batarya Kapasitesi (Örn: %80 batarya = 400 km)
        val baseRange = maxRangeKm * (telemetry.batteryLevelPct / 100.0)

        // 2. Sıcaklık Çarpanı (Klima/Isıtıcı kullanımı menzili düşürür)
        // İdeal sıcaklıktan (22 derece) her 1 derece sapma, menzili %1 etkilesin
        val tempDiff = abs(telemetry.cabinTemperatureC - optimalTemp)
        val tempPenalty = 1.0 - (tempDiff * 0.01)

        // 3. Hız Çarpanı (Yüksek hız, rüzgar direncinden dolayı menzili düşürür)
        // 90 km/h üzeri hızlarda her 10 km/h ekstra hız, menzili %5 düşürsün
        var speedPenalty = 1.0
        if (telemetry.speedKmh > 90) {
            val extraSpeed = telemetry.speedKmh - 90
            speedPenalty = 1.0 - ((extraSpeed / 10.0) * 0.05)
        }

        // 4. Rejenerasyon Katkısı (Frenleme ile kazanılan enerji)
        // Eğer araç o an enerji üretiyorsa anlık menzile %5 bonus ekle
        val regenBonus = if (telemetry.regenerationKw > 0) 1.05 else 1.0

        // Tüm fiziksel koşulları çarparak nihai menzili buluyoruz
        val finalRange = baseRange * tempPenalty * speedPenalty * regenBonus

        // Menzilin negatif bir değere düşmesini engelle ve ekranda temiz görünsün diye tam sayıya yuvarla
        return maxOf(0, finalRange.toInt())
    }
}