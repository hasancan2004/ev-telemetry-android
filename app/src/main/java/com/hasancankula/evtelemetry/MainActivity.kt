package com.hasancankula.evtelemetry

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat

import com.hasancankula.evtelemetry.presentation.TelemetryForegroundService
import com.hasancankula.evtelemetry.presentation.TelemetryViewModel
import com.hasancankula.evtelemetry.presentation.navigation.TelemetryAppNavigation
import com.hasancankula.evtelemetry.ui.theme.EVTelemetryTheme

class MainActivity : ComponentActivity() {
    private val viewModel : TelemetryViewModel by viewModels()

    // Android 13+ için bildirim izni penceresi motoru
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startTelemetryService() // İzin verildiyse nöbetçiyi başlat
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        checkPermissionsAndStartService() // YENİ: İzinleri kontrol et ve servisi başlat

        setContent {
            EVTelemetryTheme {
                TelemetryAppNavigation(viewModel = viewModel)
            }
        }
    }

    private fun checkPermissionsAndStartService() {
        // Android 13 ve üstü ise bildirim izni iste
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startTelemetryService() // Zaten izin varsa direkt başlat
            }
        } else {
            startTelemetryService() // Android 12 ve altı ise direkt başlat
        }
    }

    private fun startTelemetryService() {
        val serviceIntent = Intent(this, TelemetryForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "telemetry_alerts_channel"
            val channelName = "Filo Kritik Alarmlar"
            val descriptionText = "Yapay zeka arıza riski %75'i geçtiğinde tetiklenen acil durum bildirimleri."
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}