package com.hasancankula.evtelemetry.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.hasancankula.evtelemetry.data.EVTelemetryDto
import com.hasancankula.evtelemetry.data.TelemetryHistoryDto
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf

// ========================================================
// ANA NAVIGASYON MOTORU
// ========================================================
@Composable
fun TelemetryAppNavigation(viewModel: TelemetryViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "fleet_dashboard") {

        // 1. Ekran: Filo Listesi
        composable("fleet_dashboard") {
            FleetDashboardScreen(
                viewModel = viewModel,
                onVehicleClick = { vehicleId ->
                    viewModel.selectVehicle(vehicleId) // ViewModel'e hangi aracı seçtiğimizi söyle
                    navController.navigate("vehicle_detail/$vehicleId") // Detay sayfasına uçur
                }
            )
        }

        // 2. Ekran: Araç Detay & Canlı Harita Takibi
        composable(
            route = "vehicle_detail/{vehicleId}",
            arguments = listOf(navArgument("vehicleId") { type = NavType.StringType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""

            VehicleDetailScreen(
                vehicleId = vehicleId,
                viewModel = viewModel,
                onBackClick = {
                    viewModel.clearSelectedVehicle() // Hafızayı temizle
                    navController.popBackStack() // Geri dön
                }
            )
        }
    }
}

// ========================================================
// 1. EKRAN: FİLO GÖSTERGE PANELİ
// ========================================================
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
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
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
                        Text(text = "Hata:\n${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is FleetUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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

@Composable
fun VehicleFleetCard(vehicle: EVTelemetryDto, onClick: () -> Unit) {
    val isMoving = vehicle.speedKmh > 0

    // AI Riskine göre renk belirliyoruz
    val riskColor = when {
        vehicle.maintenanceRiskPct > 75.0 -> Color.Red
        vehicle.maintenanceRiskPct > 40.0 -> Color(0xFFFFA500) // Turuncu
        else -> Color(0xFF4CAF50) // Yeşil
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }, // Tıklanma özelliği ekledik!
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = vehicle.vehicleId, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = if (isMoving) "Aktif" else "Şarjda / Beklemede",
                    color = if (isMoving) MaterialTheme.colorScheme.primary else Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Hız: ${vehicle.speedKmh} km/h")
                Text(text = "Batarya: %${vehicle.batteryLevelPct}", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // YENİ: YAPAY ZEKA RİSK GÖSTERGESİ
            Row(
                modifier =  Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "AI Arıza Riski:", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "%${vehicle.maintenanceRiskPct}",
                    fontWeight = FontWeight.Bold,
                    color = riskColor,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

// ========================================================
// 2. EKRAN: DETAY VE CANLI HARİTA EKRANI (DÜNKÜ ZAFERİMİZ)
// ========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(vehicleId: String, viewModel: TelemetryViewModel, onBackClick: () -> Unit) {
    // Detay state'ini dinliyoruz
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "$vehicleId Detay Paneli", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            val telemetry = detailState.telemetry

            if (telemetry == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator() // Geçmiş yüklenirken döner
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sadece bu araca ait canlı harita ve polyline çizgisi
                    LiveMapCard(
                        latitude = telemetry.latitude,
                        longitude = telemetry.longitude,
                        speed = telemetry.speedKmh,
                        routeHistory = detailState.routeHistory
                    )

                    // YENİ EKLENEN GRAFİK KARTIMIZ
                    SpeedAnalyticsCard(routeHistory = detailState.routeHistory)

                    // YENİ EKLENEN YAPAY ZEKA KARTI
                    TelemetryCard(
                        title = "Yapay Zeka Arıza Riski",
                        value = "%${telemetry.maintenanceRiskPct}",
                        containerColor = if(telemetry.maintenanceRiskPct > 50.0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if(telemetry.maintenanceRiskPct > 50.0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    TelemetryCard(title = "Anlık Hız", value = "${telemetry.speedKmh} km/h", valueStyle = MaterialTheme.typography.displayMedium)
                    TelemetryCard(title = "Tahmini Menzil (AI)", value = "${detailState.estimatedRange} km", containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)
                    TelemetryCard(title = "Batarya Seviyesi", value = "%${telemetry.batteryLevelPct}")
                    TelemetryCard(title = "Kabin Sıcaklığı", value = "${telemetry.cabinTemperatureC} °C")
                    TelemetryCard(title = "Süspansiyon Modu", value = telemetry.suspensionMode)

                    ControlPanelCard(onModeSelected = { secilenMod ->
                        viewModel.setSuspensionMode(vehicleId, secilenMod) // Sadece bu araca komut gönder
                    })
                }
            }
        }
    }
}

// Harita Bileşeni (Dünkü jilet gibi çalışan kodumuz)
@Composable
fun LiveMapCard(latitude: Double, longitude: Double, speed: Int, routeHistory: List<TelemetryHistoryDto>) {
    val carLocation = LatLng(latitude, longitude)
    val polylinePoints = routeHistory.map { LatLng(it.latitude, it.longitude) }
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(carLocation, 16f) }

    LaunchedEffect(carLocation) {
        cameraPositionState.animate(CameraUpdateFactory.newLatLng(carLocation))
    }

    Card(modifier = Modifier.fillMaxWidth().height(300.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
            if (polylinePoints.isNotEmpty()) {
                Polyline(points = polylinePoints, color = Color.Blue, width = 12f)
            }
            Marker(state = MarkerState(position = carLocation), title = "Araç Konumu", snippet = "Hız: $speed km/h")
        }
    }
}

@Composable
fun ControlPanelCard(onModeSelected: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Araç Uzaktan Kontrol Paneli", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { onModeSelected("Sport") }) { Text("Sport") }
                Button(onClick = { onModeSelected("Comfort") }) { Text("Comfort") }
                Button(onClick = { onModeSelected("Eco") }) { Text("Eco") }
            }
        }
    }
}

@Composable
fun TelemetryCard(title: String, value: String, valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium, containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface, contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = contentColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = valueStyle, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SpeedAnalyticsCard(routeHistory: List<TelemetryHistoryDto>) {
    // 1. DTO listemizdeki verileri, grafiğin anlayacağı X (Zaman) ve Y (Hız) koordinatlarına çeviriyoruz
    val chartEntries = routeHistory.mapIndexed { index, dto ->
        FloatEntry(x = index.toFloat(), y = dto.speedKmh.toFloat())
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
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
            Text(
                text = "Hız Analizi (Son 100 Veri)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Eğer yeterli veri varsa grafiği çizdiriyoruz
            if (chartEntries.size > 1) {
                val chartEntryModel = entryModelOf(chartEntries)

                Chart(
                    chart = lineChart(),
                    model = chartEntryModel,
                    startAxis = rememberStartAxis(title = "Hız (km/h)"),
                    bottomAxis = rememberBottomAxis(),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                    ) {
                    Text(
                        "Grafik için veri toplanıyor...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

            }
        }

    }
}