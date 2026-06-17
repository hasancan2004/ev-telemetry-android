package com.hasancankula.evtelemetry.presentation.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument

import com.hasancankula.evtelemetry.chat.presentation.ChatScreen
import com.hasancankula.evtelemetry.presentation.Screen
import com.hasancankula.evtelemetry.presentation.TelemetryViewModel
import com.hasancankula.evtelemetry.presentation.analytics.AnalyticsScreen
import com.hasancankula.evtelemetry.presentation.detail.VehicleDetailScreen
import com.hasancankula.evtelemetry.presentation.fleet.FleetDashboardScreen
import com.hasancankula.evtelemetry.presentation.geofence.GeofenceMapScreen
import com.hasancankula.evtelemetry.presentation.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryAppNavigation(viewModel: TelemetryViewModel) {
    val navController = rememberNavController()

    val items = listOf(Screen.Fleet, Screen.Analytics, Screen.Chat, Screen.Geofence, Screen.Settings)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isDarkTheme by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val brandColor = if (isDarkTheme) Color(0xFF7B8DBC) else Color(0xFF4A5D8A)
    val unselectedIconColor = if (isDarkTheme) Color.LightGray else Color.Gray

    val isDetailScreen = currentRoute?.startsWith("vehicle_detail") == true

    Scaffold(
        topBar = {
            if (!isDetailScreen) {
                val titleText = when (currentRoute) {
                    Screen.Fleet.route -> "Filo Kontrol Merkezi"
                    Screen.Geofence.route -> "Güvenlik Bölgeleri"
                    Screen.Settings.route -> "Filo Kontrol Ayarları"
                    Screen.Analytics.route -> "Filo Performans Raporları"
                    Screen.Chat.route -> "Yapay Zeka Asistanı"
                    else -> "Filo Kontrol Merkezi"
                }

                CenterAlignedTopAppBar(
                    title = { Text(text = titleText, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant else brandColor,
                        titleContentColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                    )
                )
            }
        },
        bottomBar = {
            if (!isDetailScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    items.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) brandColor else unselectedIconColor
                                )
                            },
                            label = {
                                Text(
                                    screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) brandColor else unselectedIconColor
                                )
                            },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = if (isDarkTheme) brandColor.copy(alpha = 0.2f) else brandColor.copy(alpha = 0.1f)
                            ),
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Fleet.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isDetailScreen) PaddingValues(0.dp) else paddingValues)
        ) {
            composable(Screen.Fleet.route) {
                FleetDashboardScreen(
                    viewModel = viewModel,
                    onVehicleClick = { vehicleId ->
                        viewModel.selectVehicle(vehicleId)
                        navController.navigate("vehicle_detail/$vehicleId")
                    }
                )
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = viewModel)
            }
            composable(Screen.Chat.route) {
                ChatScreen()
            }
            composable(Screen.Geofence.route) {
                GeofenceMapScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
            composable(
                route = "vehicle_detail/{vehicleId}",
                arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
            ) {
                VehicleDetailScreen(
                    vehicleId = it.arguments?.getString("vehicleId") ?: "",
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}