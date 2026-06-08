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
                    // ViewModel'den gelen her iki veriyi de alıyoruz
                    val telemetry = state.telemetry
                    val range = state.estimatedRange

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

                        // ========================================================
                        // YENİ EKLENEN KISIM: Akıllı Menzil Kartı
                        // ========================================================
                        TelemetryCard(
                            title = "Tahmini Menzil (AI)",
                            value = "$range km",
                            valueStyle = MaterialTheme.typography.headlineLarge,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer, // Farka dikkat çekmek için farklı renk
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
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

                        ControlPanelCard(
                            onModeSelected = { secilenMod ->
                                viewModel.setSuspensionMode(secilenMod)
                            }
                        )
                    }
                }
            }
        }
    }
}

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

// TelemetryCard'ı renk parametreleri alacak şekilde ufak bir güncellemeyle geliştirdik
@Composable
fun TelemetryCard(
    title: String,
    value: String,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
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
                color = contentColor
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