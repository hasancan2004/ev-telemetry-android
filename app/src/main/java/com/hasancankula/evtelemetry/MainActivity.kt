package com.hasancankula.evtelemetry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.hasancankula.evtelemetry.presentation.TelemetryAppNavigation
import com.hasancankula.evtelemetry.presentation.TelemetryViewModel
import com.hasancankula.evtelemetry.ui.theme.EVTelemetryTheme

class MainActivity : ComponentActivity() {
    // ViewModel'i Android'in yaşam döngüsüne (Lifecycle) uygun bir şekilde yaratıyoruz.
    // Ekran yan çevrilse bile veri kaybolmaz, aynı ViewModel kullanılmaya devam eder.
    private val viewModel : TelemetryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Android Studio'nun otomatik oluşturduğu tema sarmalayıcısı
            EVTelemetryTheme {

                // Eski TelemetryScreen yerine yeni Navigasyon motorumuzu bağlıyoruz
                TelemetryAppNavigation(viewModel = viewModel)
            }
        }
    }
}
