package com.hasancankula.evtelemetry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hasancankula.evtelemetry.presentation.TelemetryScreen
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

                // Az önce tasarladığımız o efsane ekranı çağırıp, içine veri motorumuzu (ViewModel) bağlıyoruz.
                TelemetryScreen(viewModel = viewModel)
            }
        }
    }
}
