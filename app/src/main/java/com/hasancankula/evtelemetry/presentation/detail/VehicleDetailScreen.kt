package com.hasancankula.evtelemetry.presentation.detail

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.hasancankula.evtelemetry.R
import com.hasancankula.evtelemetry.data.ChargingStationDto
import com.hasancankula.evtelemetry.data.TelemetryHistoryDto
import com.hasancankula.evtelemetry.presentation.TelemetryViewModel
import com.hasancankula.evtelemetry.presentation.detail.bitmapDescriptorFromVector
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryModelOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(vehicleId: String, viewModel: TelemetryViewModel, onBackClick: () -> Unit) {
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val chargingStations by viewModel.chargingStations.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "$vehicleId Detay Paneli", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Geri") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues), color = MaterialTheme.colorScheme.surfaceVariant) {
            val telemetry = detailState.telemetry

            if (telemetry == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                // YENİ MİMARİ: Ekranı ikiye bölen Column (Scroll YOK)
                Column(modifier = Modifier.fillMaxSize()) {

                    // 1. ÜST KISIM: Sabit Harita (Ekranın %45'i)
                    Box(modifier = Modifier.weight(0.45f).fillMaxWidth()) {
                        LiveMapCard(
                            latitude = telemetry.latitude,
                            longitude = telemetry.longitude,
                            speed = telemetry.speedKmh,
                            chargingStations = chargingStations,
                            routeHistory = detailState.routeHistory
                        )
                    }

                    // 2. ALT KISIM: Kaydırılabilir Kartlar (Ekranın %55'i)
                    Column(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()) // Scroll sadece bu alt kısıma ait
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // YENİ: Eco-Score Kartını ekrana basıyoruz
                        EcoScoreCard(score = telemetry.eco_score)
                        SpeedAnalyticsCard(routeHistory = detailState.routeHistory)

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

                        ControlPanelCard(onModeSelected = { secilenMod -> viewModel.setSuspensionMode(vehicleId, secilenMod) })
                    }
                }
            }
        }
    }
}

// YENİ: Drawable'ı Marker için ikona çeviren sihirli fonksiyon
fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
    val drawable = androidx.core.content.ContextCompat.getDrawable(context, vectorResId) ?: return null
    drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    val bm = android.graphics.Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bm)
    drawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bm)
}

@Composable
fun LiveMapCard(
    latitude: Double,
    longitude: Double,
    speed: Int,
    chargingStations: List<ChargingStationDto>,
    routeHistory: List<TelemetryHistoryDto>
) {
    val context = LocalContext.current // İkonu oluşturmak için Context lazım
    val carLocation = LatLng(latitude, longitude)
    val polylinePoints = routeHistory.map { LatLng(it.latitude, it.longitude) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(carLocation, 13f)
    }

    var isFollowingCar by remember { mutableStateOf(true) }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            isFollowingCar = false
        }
    }

    LaunchedEffect(carLocation) {
        if (isFollowingCar) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLng(carLocation))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            if (polylinePoints.isNotEmpty()) Polyline(points = polylinePoints, color = Color.Blue, width = 12f)

            // Araç Markeri
            Marker(state = MarkerState(position = carLocation), title = "Araç Konumu", snippet = "Hız: $speed km/h")

            // YENİ: Şarj İstasyonlarını Özel İkonla Çiz
            chargingStations.forEach { station ->

                // 1. Önce senin az önce oluşturduğun havalı ikonu (ic_charging_station) bulmaya çalışıyoruz.
                // 2. Eğer bulamazsa (veya adını yanlış yazdıysan), veri geldiğini kanıtlamak için MAVİ DAMLA koyacak.
                val customIcon = bitmapDescriptorFromVector(context, R.drawable.ic_charging_station)
                    ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)

                Marker(
                    state = MarkerState(position = LatLng(station.latitude, station.longitude)),
                    title = "${station.provider} - ${station.name}",
                    snippet = if (station.is_available) "Müsait (Hızlı Şarj)" else "Dolu / Arızalı",
                    icon = customIcon
                )
            }
        }

        if (!isFollowingCar) {
            FloatingActionButton(
                onClick = { isFollowingCar = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(44.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Araca Odaklan",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ControlPanelCard(onModeSelected: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
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
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = contentColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = valueStyle, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SpeedAnalyticsCard(routeHistory: List<TelemetryHistoryDto>) {
    val chartEntries = routeHistory.mapIndexed { index, dto -> FloatEntry(x = index.toFloat(), y = dto.speedKmh.toFloat()) }
    Card(modifier = Modifier.fillMaxWidth().height(250.dp).padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(text = "Hız Analizi (Son 100 Veri)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            if (chartEntries.size > 1) {
                val chartEntryModel = entryModelOf(chartEntries)
                Chart(chart = lineChart(), model = chartEntryModel, startAxis = rememberStartAxis(title = "Hız (km/h)"), bottomAxis = rememberBottomAxis(), modifier = Modifier.fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Grafik için veri toplanıyor...", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
fun EcoScoreCard(score: Int) {
    // Skora göre renk ve metin belirleme mantığı
    val (statusColor, statusText) = when {
        score >= 90 -> Color(0xFF4CAF50) to "Mükemmel - Verimli Sürüş"
        score >= 70 -> Color(0xFF8BC34A) to "İyi - Standart Sürüş"
        score >= 50 -> Color(0xFFFFC107) to "Orta - Dikkat Edilmeli"
        else -> Color(0xFFF44336) to "Agresif Sürüş - Yüksek Tüketim"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Eco-Score (Sürücü Analizi)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Dairesel Gösterge (Gauge)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                CircularProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = statusColor,
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Durum Metni
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = statusColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}