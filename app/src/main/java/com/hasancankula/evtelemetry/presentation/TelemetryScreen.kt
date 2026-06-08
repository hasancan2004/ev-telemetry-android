package com.hasancankula.evtelemetry.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// GOOGLE MAPS KÜTÜPHANELERİ VE YENİ POLYLINE IMPORTU
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.hasancankula.evtelemetry.data.TelemetryHistoryDto

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
                    val range = state.estimatedRange
                    // YENİ: ViewModel'den geçmiş rotayı alıyoruz
                    val history = state.routeHistory

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // YENİ: Geçmiş rotayı (history) harita kartımıza gönderiyoruz
                        LiveMapCard(
                            latitude = telemetry.latitude,
                            longitude = telemetry.longitude,
                            speed = telemetry.speedKmh,
                            routeHistory = history
                        )

                        TelemetryCard(
                            title = "Anlık Hız",
                            value = "${telemetry.speedKmh} km/h",
                            valueStyle = MaterialTheme.typography.displayMedium
                        )

                        TelemetryCard(
                            title = "Tahmini Menzil (AI)",
                            value = "$range km",
                            valueStyle = MaterialTheme.typography.headlineLarge,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
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

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ========================================================
// GÜNCELLENEN HARİTA BİLEŞENİMİZ (POLYLINE EKLENDİ)
// ========================================================
@Composable
fun LiveMapCard(
    latitude: Double,
    longitude: Double,
    speed: Int,
    routeHistory: List<TelemetryHistoryDto> // YENİ PARAMETRE
) {
    val carLocation = LatLng(latitude, longitude)

    // Geçmiş rotadaki (DTO içindeki) noktaları Google Maps'in anladığı LatLng listesine çeviriyoruz
    val polylinePoints = routeHistory.map { LatLng(it.latitude, it.longitude) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(carLocation, 16f) // Biraz daha yaklaştırdık ki çizgi net görünsün
    }

    LaunchedEffect(carLocation) {
        cameraPositionState.animate(CameraUpdateFactory.newLatLng(carLocation))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // YENİ: Aracın arkasından gelen o havalı mavi rota çizgisi
            if (polylinePoints.isNotEmpty()) {
                Polyline(
                    points = polylinePoints,
                    color = Color.Blue,
                    width = 12f // Çizginin kalınlığı
                )
            }

            // Aracın güncel konumu
            Marker(
                state = MarkerState(position = carLocation),
                title = "Araç Konumu",
                snippet = "Hız: $speed km/h"
            )
        }
    }
}

@Composable
fun ControlPanelCard(onModeSelected: (String) -> Unit) {
    // ... (Aynı kalıyor)
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
                Button(onClick = { onModeSelected("Sport") }) { Text("Sport") }
                Button(onClick = { onModeSelected("Comfort") }) { Text("Comfort") }
                Button(onClick = { onModeSelected("Eco") }) { Text("Eco") }
            }
        }
    }
}

@Composable
fun TelemetryCard(
    title: String,
    value: String,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {
    // ... (Aynı kalıyor)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = contentColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = valueStyle, fontWeight = FontWeight.Bold)
        }
    }
}