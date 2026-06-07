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
    // Arka plandan gelen canlı veri nehrini dinliyoruz
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Scaffold: Bize üst bar (TopAppBar) ve modern bir iskelet sağlar
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "EV Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary, // Barın arka plan rengi
                    titleContentColor = MaterialTheme.colorScheme.onPrimary // Yazı rengi
                )
            )
        }
    ) { paddingValues ->
        // Ekranın geri kalanı. Scaffold'un bar için ayırdığı boşluğu (paddingValues) buraya veriyoruz
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.surfaceVariant // Hafif gri/farklı bir arka plan tonu
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

                    // Verileri alt alta, aralarında boşluk olacak şekilde (spacedBy) diziyoruz
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Ana Hız Kartı (Daha büyük fontla)
                        TelemetryCard(
                            title = "Anlık Hız",
                            value = "${telemetry.speedKmh} km/h",
                            valueStyle = MaterialTheme.typography.displayMedium
                        )

                        // Diğer Telemetri Kartları
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

                        TelemetryCard(
                            title = "Rejenerasyon & Lastik",
                            value = "${telemetry.regenerationKw} kW  |  ${telemetry.tirePressurePsi} PSI"
                        )
                    }
                }
            }
        }
    }
}

// Kod tekrarını önlemek ve tasarımı tek yerden yönetmek için kendi "Kart" bileşenimizi oluşturduk
@Composable
fun TelemetryCard(
    title: String,
    value: String,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp), // Köşeleri yumuşatıyoruz
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), // Karta gölge (derinlik) veriyoruz
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