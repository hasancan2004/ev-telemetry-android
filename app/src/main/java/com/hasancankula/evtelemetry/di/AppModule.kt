package com.hasancankula.evtelemetry.di

import com.hasancankula.evtelemetry.data.TelemetrySocketService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}