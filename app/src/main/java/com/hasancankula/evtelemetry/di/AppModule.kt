package com.hasancankula.evtelemetry.di

import android.content.Context
import androidx.room.Room
import com.hasancankula.evtelemetry.data.local.TelemetryDao
import com.hasancankula.evtelemetry.data.TelemetrySocketService
import com.hasancankula.evtelemetry.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTelemetrySocketService(): TelemetrySocketService {
        return TelemetrySocketService()
    }

    // YENİ: Room Veritabanını Hilt'e Öğretiyoruz
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ev_telemetry_db"
        ).fallbackToDestructiveMigration().build()
    }

    // YENİ: DAO Sınıfını Hilt'e Öğretiyoruz
    @Provides
    @Singleton
    fun provideTelemetryDao(database: AppDatabase): TelemetryDao {
        return database.telemetryDao
    }
}