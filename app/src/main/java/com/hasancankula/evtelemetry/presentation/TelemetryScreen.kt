package com.hasancankula.evtelemetry.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryScreen(viewModel: TelemetryViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "EV Dashboard", fontWeight = FontWeight.Bold)
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
                is TelemetryUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is TelemetryUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Bağlantı Hatası:\n${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                is TelemetryUiState.Success -> {
                    val telemetry = state.telemetry

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TelemetryCard(
                            title = "Anlık Hız",
                            value = "${telemetry.speedKmh} km/h",
                            valueStyle = MaterialTheme.typography.displayMedium
                        )

                        TelemetryCard(
                            title = "Batarya Seviyesi",
                            value = "%${telemetry.batteryLevelPct}"
                        )

                        TelemetryCard(
                            title = "Kabin Sıcaklığı",
                            value = "${telemetry.cabinTemperatureC} °C"
                        )

                        TelemetryCard(
                            title = "Süspansiyon Modu",
                            value = telemetry.suspensionMode
                        )

                        // ========================================================
                        // YENİ EKLENEN KISIM: Uzaktan Kumanda Panelimiz
                        // ========================================================
                        ControlPanelCard(
                            onModeSelected = { secilenMod ->
                                // Tıklanan butonun adını ViewModel'e fırlatıyoruz
                                viewModel.setSuspensionMode(secilenMod)
                            }
                        )
                    }
                }
            }
        }
    }
}

// Süspansiyon kontrol butonlarını barındıran yepyeni kart tasarımımız
@Composable
fun ControlPanelCard(onModeSelected: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Araç Kontrol Paneli",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Yan yana 3 şık buton diziyoruz
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { onModeSelected("Sport") }) {
                    Text("Sport")
                }

                Button(onClick = { onModeSelected("Comfort") }) {
                    Text("Comfort")
                }

                Button(onClick = { onModeSelected("Eco") }) {
                    Text("Eco")
                }
            }
        }
    }
}

// Eski TelemetryCard fonksiyonumuz aynı şekilde duruyor
@Composable
fun TelemetryCard(
    title: String,
    value: String,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium
) {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = valueStyle,
                fontWeight = FontWeight.Bold
            )
        }
    }
}