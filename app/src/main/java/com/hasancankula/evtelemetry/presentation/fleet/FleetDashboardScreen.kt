package com.hasancankula.evtelemetry.presentation.fleet

import androidx.compose.foundation.Canvas
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

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {
        when (val state = uiState) {
            is FleetUiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is FleetUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = "Hata:\n${state.message}", color = MaterialTheme.colorScheme.error) }
            is FleetUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    FleetSummaryPanel(vehicles = state.vehicles)

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(state.vehicles) { vehicle ->
                            VehicleFleetCard(vehicle = vehicle, onClick = { onVehicleClick(vehicle.vehicleId) })
                        }
                    }
                }
            }
        }
    }
}

// ======================================================================
// YENİ: Filonun genel durumunu gösteren 3'lü Özet Paneli
// ======================================================================
@Composable
fun FleetSummaryPanel(vehicles: List<EVTelemetryDto>) {
    val totalVehicles = vehicles.size
    val activeVehicles = vehicles.count { it.speedKmh > 0 }
    val avgBattery = if (vehicles.isNotEmpty()) vehicles.map { it.batteryLevelPct }.average().toInt() else 0
    val criticalVehicles = vehicles.count { it.maintenanceRiskPct > 75.0 || it.geofenceBreach }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(title = "Aktif", value = "$activeVehicles/$totalVehicles", color = Color(0xFF4CAF50), modifier = Modifier.weight(1f))
        SummaryCard(title = "Ort. Batarya", value = "%$avgBattery", color = Color(0xFF2196F3), modifier = Modifier.weight(1f))
        SummaryCard(title = "Kritik", value = "$criticalVehicles", color = if(criticalVehicles > 0) Color.Red else Color.Gray, modifier = Modifier.weight(1f))
    }
}

@Composable
fun SummaryCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

// ======================================================================
// GÜNCELLENDİ: Gerçek bir kurumsal karta dönüşen Araç Detay Kartı
// ======================================================================
@Composable
fun VehicleFleetCard(vehicle: EVTelemetryDto, onClick: () -> Unit) {
    // Aracın genel sağlığına göre bir durum rengi (Status Dot) belirliyoruz
    val statusColor = when {
        vehicle.geofenceBreach || vehicle.maintenanceRiskPct > 75.0 -> Color.Red
        vehicle.batteryLevelPct < 20.0 -> Color(0xFFFFA500) // Turuncu
        else -> Color(0xFF4CAF50) // Yeşil
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Başlık ve Durum Noktası
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) { drawCircle(color = statusColor) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = vehicle.vehicleModel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = " (${vehicle.vehicleId})", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }

                // İhlal varsa kocaman kırmızı uyarı yaz
                if(vehicle.geofenceBreach) {
                    Text(text = "İHLAL!", color = Color.Red, fontWeight = FontWeight.ExtraBold)
                } else {
                    Text(text = "${vehicle.speedKmh} km/h", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Batarya Çubuğu (Mini Progress Bar)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Batarya:", style = MaterialTheme.typography.bodyMedium)
                Text(text = "%${vehicle.batteryLevelPct.toInt()}", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (vehicle.batteryLevelPct / 100.0).toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (vehicle.batteryLevelPct > 20) Color(0xFF4CAF50) else Color.Red,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Sürücü ve AI Skorları
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Eco-Score", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text(text = "${vehicle.ecoScore}/100", fontWeight = FontWeight.Bold, color = if(vehicle.ecoScore > 70) Color(0xFF4CAF50) else Color(0xFFFFA500))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "AI Arıza Riski", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text(text = "%${vehicle.maintenanceRiskPct}", fontWeight = FontWeight.Bold, color = if(vehicle.maintenanceRiskPct > 50) Color.Red else MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}