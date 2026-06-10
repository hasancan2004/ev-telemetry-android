package com.hasancankula.evtelemetry.presentation.fleet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hasancankula.evtelemetry.data.EVTelemetryDto
import com.hasancankula.evtelemetry.presentation.FleetUiState
import com.hasancankula.evtelemetry.presentation.TelemetryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetDashboardScreen(viewModel: TelemetryViewModel, onVehicleClick: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Filo Kontrol Merkezi", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues), color = MaterialTheme.colorScheme.surfaceVariant) {
            when (val state = uiState) {
                is FleetUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is FleetUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = "Hata:\n${state.message}", color = MaterialTheme.colorScheme.error) }
                is FleetUiState.Success -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(state.vehicles) { vehicle ->
                            VehicleFleetCard(vehicle = vehicle, onClick = { onVehicleClick(vehicle.vehicleId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleFleetCard(vehicle: EVTelemetryDto, onClick: () -> Unit) {
    val isMoving = vehicle.speedKmh > 0
    val riskColor = when {
        vehicle.maintenanceRiskPct > 75.0 -> Color.Red
        vehicle.maintenanceRiskPct > 40.0 -> Color(0xFFFFA500)
        else -> Color(0xFF4CAF50)
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = vehicle.vehicleId, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(text = if (isMoving) "Aktif" else "Şarjda / Beklemede", color = if (isMoving) MaterialTheme.colorScheme.primary else Color.Red, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Hız: ${vehicle.speedKmh} km/h")
                Text(text = "Batarya: %${vehicle.batteryLevelPct}", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "AI Arıza Riski:", style = MaterialTheme.typography.labelLarge)
                Text(text = "%${vehicle.maintenanceRiskPct}", fontWeight = FontWeight.Bold, color = riskColor, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}