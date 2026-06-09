package com.hasancankula.evtelemetry.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hasancankula.evtelemetry.data.EVTelemetryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryScreen(viewModel: TelemetryViewModel) {
    // ViewModel'den gelen FleetUiState'i dinliyoruz
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Filo Kontrol Merkezi", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            when (val state = uiState) {
                is FleetUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is FleetUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Bağlantı Hatası:\n${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                is FleetUiState.Success -> {
                    // ARTIK ELİMİZDE BİR LİSTE VAR!
                    // LazyColumn kullanarak araçları alt alta dinamik bir şekilde diziyoruz
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.vehicles) { vehicle ->
                            VehicleFleetCard(vehicle = vehicle)
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// YENİ BİLEŞEN: FİLO ARAÇ KARTI
// ========================================================
@Composable
fun VehicleFleetCard(vehicle: EVTelemetryDto) {
    // Aracın hızına göre yolda mı yoksa şarjda mı olduğunu anlıyoruz
    val isMoving = vehicle.speedKmh > 0
    val statusText = if (isMoving) "Aktif (Yolda)" else "Beklemede / Şarjda"
    val statusColor = if (isMoving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ÜST KISIM: Plaka (ID) ve Durum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = vehicle.vehicleId, // Python'dan gelen EV-001, EV-002 vb.
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // ALT KISIM: Kritik Telemetri Verileri
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Hız", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "${vehicle.speedKmh} km/h",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Batarya", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = "%${vehicle.batteryLevelPct}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (vehicle.batteryLevelPct < 20.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}