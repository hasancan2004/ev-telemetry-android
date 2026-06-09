package com.hasancankula.evtelemetry.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name =  "fleet_settings")
class SettingsDataStore(private val context: Context) {

    // Anahtarlarımızı (Keys) belirliyoruz
    companion object {
        val AI_THRESHOLD_KEY = floatPreferencesKey("ai_alarm_threshold")
        val GEOFENCE_RADIUS_KEY = floatPreferencesKey("geofence_radius_km")
    }

    // 1. Ayarları Okuma (Flow tüneli)
    val aiThresholdFlow : Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[AI_THRESHOLD_KEY] ?: 75f // Kayıtlı yoksa varsayılan 75
        }

    val geofenceRadiusFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[GEOFENCE_RADIUS_KEY] ?: 20f // Kayıtlı yoksa varsayılan 20 km
        }

    // 2. Ayarları Yazma (Kaydetme)
    suspend fun saveAiThreshold(threshold: Float) {
        context.dataStore.edit { preferences ->
            preferences[AI_THRESHOLD_KEY] = threshold
        }
    }

    suspend fun saveGeofenceRadius(radiusKm: Float) {
        context.dataStore.edit { preferences ->
            preferences[GEOFENCE_RADIUS_KEY] = radiusKm
        }
    }

}

