package com.hasancankula.evtelemetry.presentation.geofence

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.hasancankula.evtelemetry.presentation.FleetUiState
import com.hasancankula.evtelemetry.presentation.TelemetryViewModel

@Composable
fun GeofenceMapScreen(viewModel: TelemetryViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val centerPoint = LatLng(37.8746, 32.4933)
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(centerPoint, 10f) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
            Circle(
                center = centerPoint,
                radius = 20000.0,
                fillColor = Color.Red.copy(alpha = 0.2f),
                strokeColor = Color.Red,
                strokeWidth = 5f
            )
            if (uiState is FleetUiState.Success) {
                (uiState as FleetUiState.Success).vehicles.forEach { vehicle ->
                    Marker(
                        state = MarkerState(position = LatLng(vehicle.latitude, vehicle.longitude)),
                        title = vehicle.vehicleId,
                        snippet = "Hız: ${vehicle.speedKmh} km/h"
                    )
                }
            }
        }
    }
}