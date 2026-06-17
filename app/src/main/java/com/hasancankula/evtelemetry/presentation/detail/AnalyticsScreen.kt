package com.hasancankula.evtelemetry.presentation.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect // YENİ EKLENDİ
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle // YENİ EKLENDİ
import com.hasancankula.evtelemetry.data.AnalyticsKpiDto
import com.hasancankula.evtelemetry.presentation.TelemetryViewModel // YENİ EKLENDİ
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// YENİ: Doğrudan ViewModel'i alıyoruz ki ekran her açıldığında veri çekebilelim
fun AnalyticsScreen(viewModel: TelemetryViewModel) {

    // YENİ: Veriyi state olarak dinliyoruz
    val analyticsData by viewModel.analyticsData.collectAsStateWithLifecycle()

    // YENİ: Ekran her açıldığında (Composition başladığında) verileri API'den tazelemek için tetikliyoruz
    LaunchedEffect(key1 = true) {
        viewModel.fetchAnalytics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Filo Analiz ve Raporlama", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4A5D8A),
                    titleContentColor = Color.White
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
            if (analyticsData == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4A5D8A))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Genel Filo Performans Özeti",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A5D8A)
                    )

                    // KPI Kartları
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        KpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Toplam Tüketim",
                            value = "${analyticsData?.total_energy_kwh ?: 0.0} kWh",
                            color = Color(0xFF2196F3)
                        )
                        KpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Kritik Riskli Araç",
                            value = "${analyticsData?.critical_risk_count ?: 0}",
                            color = Color(0xFFF44336)
                        )
                    }

                    KpiCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Filo Ortalama Eco-Score",
                        value = "${analyticsData?.avg_eco_score ?: 0} / 100",
                        color = if ((analyticsData?.avg_eco_score ?: 0) >= 80) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Haftalık Enerji Tüketim Trendi (Simüle)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A5D8A)
                    )

                    // Vico Sütun Grafiği (Trend Analizi)
                    TrendChartCard()
                }
            }
        }
    }
}

@Composable
fun KpiCard(modifier: Modifier = Modifier, title: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TrendChartCard() {
    val chartEntryModel = entryModelOf(
        1f to 120f,
        2f to 150f,
        3f to 110f,
        4f to 180f,
        5f to 130f,
        6f to 160f,
        7f to 140f
    )

    Card(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Chart(
                chart = columnChart(),
                model = chartEntryModel,
                startAxis = rememberStartAxis(title = "kWh"),
                bottomAxis = rememberBottomAxis(title = "Günler (Son 1 Hafta)"),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}