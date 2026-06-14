package com.hasancankula.evtelemetry.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name =  "fleet_settings")
class SettingsDataStore(private val context: Context) {

    companion object {
        val AI_THRESHOLD_KEY = floatPreferencesKey("ai_alarm_threshold")
        val GEOFENCE_RADIUS_KEY = floatPreferencesKey("geofence_radius_km")
        // YENİ: Karanlık mod anahtarı
        val DARK_MODE_KEY = booleanPreferencesKey("is_dark_mode")
    }

    val aiThresholdFlow : Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[AI_THRESHOLD_KEY] ?: 75f
        }

    val geofenceRadiusFlow: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[GEOFENCE_RADIUS_KEY] ?: 20f
        }

    // YENİ: Karanlık modu okuma (Varsayılan olarak aydınlık mod - false)
    val isDarkModeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

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

    // YENİ: Karanlık modu kaydetme
    suspend fun saveDarkMode(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDark
        }
    }
}