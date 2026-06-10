package com.hasancankula.evtelemetry.presentation

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hasancankula.evtelemetry.data.SettingsDataStore
import com.hasancankula.evtelemetry.data.TelemetrySocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TelemetryForegroundService : Service() {

    private lateinit var settingsDataStore: SettingsDataStore

    // Dinamik olarak güncellenecek AI sınırı
    private var currentAiThreshold = 75.0
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val socketService = TelemetrySocketService()
    private val channelId = "telemetry_alerts_channel"

    // Spam engelleme hafızaları (Araç ID'si ve Son Bildirim Zamanı)
    private val lastAiAlertTime = mutableMapOf<String, Long>()
    private val lastGeofenceAlertTime = mutableMapOf<String, Long>()

    // SOĞUMA SÜRESİ: Aynı araca arka arkaya bildirim atmak için geçmesi gereken süre (1 Dakika)
    private val COOLDOWN_MS = 60 * 1000L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        settingsDataStore = SettingsDataStore(this)

        // 1. Yapay Zeka sınırını sürekli dinle
        serviceScope.launch {
            settingsDataStore.aiThresholdFlow.collect { threshold ->
                currentAiThreshold = threshold.toDouble()
            }
        }

        // NOT: Geofence sınırını (Meters) ve CenterLat/Lng değişkenlerini sildik!
        // Çünkü bu hesaplamanın tamamını artık Python Backend'i yapıyor.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Filo Takip Servisi Aktif")
            .setContentText("Yapay zeka ve Coğrafi Sınır (Geofence) koruması devrede...")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .build()

        startForeground(1, notification)
        startMonitoring()

        return START_STICKY
    }

    private fun startMonitoring() {
        serviceScope.launch {
            socketService.getTelemetryStream()
                .catch { e -> e.printStackTrace() }
                .collect { fleetList ->
                    val currentTime = System.currentTimeMillis()

                    fleetList.forEach { vehicle ->

                        // ========================================================
                        // 1. KONTROL: YAPAY ZEKA ARIZA RİSKİ
                        // ========================================================
                        if (vehicle.maintenanceRiskPct > currentAiThreshold) {
                            val lastAlertTime = lastAiAlertTime[vehicle.vehicleId] ?: 0L

                            if (currentTime - lastAlertTime > COOLDOWN_MS) {
                                sendCriticalAlert(vehicle.vehicleId, vehicle.maintenanceRiskPct)
                                lastAiAlertTime[vehicle.vehicleId] = currentTime
                            }
                        }

                        // ========================================================
                        // 2. KONTROL: COĞRAFİ SINIR İHLALİ (GEOFENCING)
                        // Backend'den gelen hazır geofenceBreach boolean değerini kullanıyoruz.
                        // ========================================================
                        if (vehicle.geofenceBreach) {
                            val lastGeofenceTime = lastGeofenceAlertTime[vehicle.vehicleId] ?: 0L

                            if (currentTime - lastGeofenceTime > COOLDOWN_MS) {
                                sendGeofenceAlert(vehicle.vehicleId, vehicle.vehicleModel)
                                lastGeofenceAlertTime[vehicle.vehicleId] = currentTime
                            }
                        }
                    }
                }
        }
    }

    private fun sendCriticalAlert(vehicleId: String, risk: Double) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alertNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("🚨 KRİTİK YAPAY ZEKA UYARISI")
            .setContentText("$vehicleId aracında yüksek arıza riski tespit edildi! (%$risk)")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(vehicleId.hashCode(), alertNotification)
    }

    private fun sendGeofenceAlert(vehicleId: String, vehicleModel: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val geofenceNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("⚠️ BÖLGE İHLALİ ALARMI")
            .setContentText("$vehicleModel ($vehicleId) operasyon bölgesini TERK ETTİ!")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(1000, 1000, 1000)) // Telefonu titret
            .setAutoCancel(true)
            .build()

        // Sınır ihlallerinin ID'sini ayırıyoruz ki aynı aracın hem AI hem sınır bildirimi aynı anda ekranda kalabilsin
        notificationManager.notify((vehicleId + "_geofence").hashCode(), geofenceNotification)
    }

    override fun onDestroy() {
        super.onDestroy()
        socketService.closeClient()
    }
}