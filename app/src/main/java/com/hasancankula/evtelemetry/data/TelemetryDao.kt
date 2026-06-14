package com.hasancankula.evtelemetry.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TelemetryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetries(telemetries: List<TelemetryEntity>)

    // HARF HATASI DÜZELTİLDİ: Telemeries -> Telemetries
    @Query("SELECT * FROM telemetry_logs")
    fun getAllTelemetriesFlow(): Flow<List<TelemetryEntity>>

    @Query("DELETE FROM telemetry_logs")
    suspend fun clearAll()
}