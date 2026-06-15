package com.hasancankula.evtelemetry.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hasancankula.evtelemetry.presentation.TelemetryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TelemetryViewModel) {
    val aiThreshold by viewModel.aiAlarmThreshold.collectAsStateWithLifecycle()
    val geofenceRadius by viewModel.geofenceRadiusKm.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Text(text = "Görünüm", style = MaterialTheme.typography.titleLarge, color = Color(0xFF4A5D8A), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Karanlık Mod", fontWeight = FontWeight.Bold)
                    Text(text = "Uygulama temasını gece moduna geçirir.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.updateDarkMode(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4A5D8A))
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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

        // YENİ: Sistem Kontrolü (Patron Modu) Kartı
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Sistem Kontrolü (Patron Modu)", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tüm Filonun Şarjını Sıfırla", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Simülasyondaki tüm araçların bataryasını anında %100 yapar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    Button(
                        onClick = { viewModel.resetFleetBatteries() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                    ) {
                        Text("Sıfırla")
                    }
                }
            }
        }
    }
}