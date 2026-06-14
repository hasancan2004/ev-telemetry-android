package com.hasancankula.evtelemetry.data.local

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(
    entities = [TelemetryEntity::class],
    version = 1,
    exportSchema = false
)

abstract class AppDatabase : RoomDatabase(){
    abstract val telemetryDao: TelemetryDao
}