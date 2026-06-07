package com.hasancankula.evtelemetry.presentation

import android.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.isPopupLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TelemetryScreen(viewModel: TelemetryViewModel) {
    // Arka planda akan veri nehrini (StateFlow), Compose'un anlayacağı ve
    // her değiştiğinde ekranı baştan çizeceği bir State'e (Durum) dönüştürüp dinliyoruz.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Ekranın genel tasarımı (Tam ekran yapıp, arka plan rengini veriyoruz)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // uiState'in o anki durumuna göre Kotlin'in harika 'when' yapısıyla ekranı çiziyoruz
        when (val state = uiState) {

            // 1. DURUM: Veri henüz gelmediyse veya bağlanmaya çalışıyorsa
            TelemetryUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator() // Dönen yükleme çarkı
                }
            }

            // 2. DURUM: Python sunucusu kapalıysa veya bağlantı koptuysa
            is TelemetryUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Bağlantı Hatası:\n${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // 3. DURUM: Veri başarıyla şelaleden akıyorsa (Şov kısmı)
            is TelemetryUiState.Success -> {
                val telemetry = state.telemetry  // Gelen JSON verimiz

                // Verileri alt alta ve ekranın tam ortasına hizalayarak diziyoruz
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "EV Telemetry Canlı İzleme",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(text = "Hız: ${telemetry.speedKmh} km/h", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Batarya: %${telemetry.batteryLevelPct}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Kabin Sıcaklığı: ${telemetry.cabinTemperatureC} °C", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Süspansiyon: ${telemetry.suspensionMode}", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Rejenerasyon: ${telemetry.regenerationKw} kW", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Lastik Basıncı: ${telemetry.tirePressurePsi} PSI", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

}