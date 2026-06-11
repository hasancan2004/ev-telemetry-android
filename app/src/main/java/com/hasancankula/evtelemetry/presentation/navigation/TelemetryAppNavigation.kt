package com.hasancankula.evtelemetry.presentation.navigation

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
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.hasancankula.evtelemetry.presentation.Screen
import com.hasancankula.evtelemetry.presentation.TelemetryViewModel
import com.hasancankula.evtelemetry.presentation.detail.VehicleDetailScreen
import com.hasancankula.evtelemetry.presentation.fleet.FleetDashboardScreen
import com.hasancankula.evtelemetry.presentation.geofence.GeofenceMapScreen
import com.hasancankula.evtelemetry.presentation.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryAppNavigation(viewModel: TelemetryViewModel) {
    val navController = rememberNavController()
    val items = listOf(Screen.Fleet, Screen.Geofence, Screen.Settings)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Kurumsal Tema Rengimiz (Tüm ekranlarda sabit lacivert)
    val brandColor = Color(0xFF4A5D8A)

    // Detay ekranındayken ana TopBar ve BottomBar'ı gizlemek için kontrol
    val isDetailScreen = currentRoute?.startsWith("vehicle_detail") == true

    Scaffold(
        topBar = {
            if (!isDetailScreen) {
                // Dinamik başlık belirleme mantığı
                val titleText = when (currentRoute) {
                    Screen.Fleet.route -> "Filo Kontrol Merkezi"
                    Screen.Geofence.route -> "Güvenlik Bölgeleri"
                    Screen.Settings.route -> "Filo Kontrol Ayarları"
                    else -> "Filo Kontrol Merkezi"
                }

                CenterAlignedTopAppBar(
                    title = { Text(text = titleText, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = brandColor,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            if (!isDetailScreen) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    items.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) brandColor else Color.Gray
                                )
                            },
                            label = {
                                Text(
                                    screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) brandColor else Color.Gray
                                )
                            },
                            selected = isSelected,
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