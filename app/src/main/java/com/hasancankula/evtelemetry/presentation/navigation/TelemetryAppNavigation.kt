package com.hasancankula.evtelemetry.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

@Composable
fun TelemetryAppNavigation(viewModel: TelemetryViewModel) {
    val navController = rememberNavController()
    val items = listOf(Screen.Fleet, Screen.Geofence, Screen.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Fleet.route,
            modifier = Modifier.padding(paddingValues)
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