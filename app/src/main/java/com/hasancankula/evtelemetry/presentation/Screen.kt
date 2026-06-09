package com.hasancankula.evtelemetry.presentation

import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Fleet : Screen("fleet", "Filo", androidx.compose.material.icons.Icons.Default.List)
    object Geofence : Screen("geofence", "Güvenlik", androidx.compose.material.icons.Icons.Default.GppGood)
    object Settings : Screen("settings", "Ayarlar", androidx.compose.material.icons.Icons.Default.Settings)
}