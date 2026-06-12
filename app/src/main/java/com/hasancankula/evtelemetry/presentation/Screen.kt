package com.hasancankula.evtelemetry.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Fleet : Screen("fleet", "Filo", Icons.Default.List)
    object Analytics : Screen("analytics", "Raporlar", Icons.Default.Info)
    object Geofence : Screen("geofence", "Güvenlik", Icons.Default.GppGood)
    object Settings : Screen("settings", "Ayarlar", Icons.Default.Settings)
}