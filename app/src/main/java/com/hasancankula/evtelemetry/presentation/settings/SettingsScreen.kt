package com.hasancankula.evtelemetry.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hasancankula.evtelemetry.presentation.TelemetryViewModel

@OptIn
@Composable
fun SettingsScreen(viewModel: TelemetryViewModel) {
    val aiThreshold by viewModel.aiAlarmThreshold.collectAsStateWithLifecycle()
    val geofenceRadius by viewModel.geofenceRadiusKm.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Bildirim Hassasiyeti", style = MaterialTheme.typography.titleLarge, color = Color(0xFF4A5D8A), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Yapay Zeka Alarm Eşiği", fontWeight = FontWeight.Bold)
                    Text(text = "%${aiThreshold.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Arıza riski bu seviyeyi aştığında acil durum bildirimi gönderilir.", style = MaterialTheme.typography.bodySmall)
                Slider(value = aiThreshold, onValueChange = { viewModel.updateAiThreshold(it) }, valueRange = 50f..95f, steps = 8)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Coğrafi Sınır (Geofencing)", style = MaterialTheme.typography.titleLarge, color = Color(0xFF4A5D8A), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Güvenli Bölge Çapı", fontWeight = FontWeight.Bold)
                    Text(text = "${geofenceRadius.toInt()} km", fontWeight = FontWeight.Bold, color = Color(0xFF4A5D8A))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Araçlar Konya merkezinden bu mesafe kadar uzaklaştığında sınır ihlali sayılır.", style = MaterialTheme.typography.bodySmall)
                Slider(value = geofenceRadius, onValueChange = { viewModel.updateGeofenceRadius(it) }, valueRange = 5f..100f, steps = 18)
            }
        }
    }
}