package com.hasancankula.evtelemetry.presentation

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hasancankula.evtelemetry.data.TelemetrySocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class TelemetryForegroundService : Service() {

    // Arka plan işlemleri için kendi scope'umuzu oluşturuyoruz
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val socketService = TelemetrySocketService()

    // MainActivity'de oluşturduğumuz kanalın ID'si
    private val channelId = "telemetry_alerts_channel"

    // KRİTİK: Spam yapmamak için bildirim atılan araçları aklımızda tutuyoruz
    private val alertedVehicles = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Android'e "Ben arka planda meşru bir iş yapıyorum, beni öldürme" demek için sabit bildirim
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Filo Takip Servisi Aktif")
            .setContentText("Yapay zeka arka planda araçları izliyor...")
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Varsayılan ikon
            .build()

        // Servisi ön plana (Foreground) alıyoruz
        startForeground(1, notification)

        // 2. Python sunucusuna tünel açıp dinlemeye başla
        startMonitoring()

        // Sistem servisi öldürürse yeniden başlatması için START_STICKY dönüyoruz
        return START_STICKY
    }

    private fun startMonitoring() {
        serviceScope.launch {
            socketService.getTelemetryStream()
                .catch { e -> e.printStackTrace() }
                .collect { fleetList ->
                    fleetList.forEach { vehicle ->
                        // Yapay Zeka Arıza Riski %75'i geçtiyse Alarm Ver!
                        if (vehicle.maintenanceRiskPct > 75.0) {
                            if (!alertedVehicles.contains(vehicle.vehicleId)) {
                                sendCriticalAlert(vehicle.vehicleId, vehicle.maintenanceRiskPct)
                                alertedVehicles.add(vehicle.vehicleId) // Hafızaya al, bir daha spam atma
                            }
                        } else {
                            // Risk düştüyse hafızadan sil ki ileride tekrar bozulursa haber versin
                            alertedVehicles.remove(vehicle.vehicleId)
                        }
                    }
                }
        }
    }

    private fun sendCriticalAlert(vehicleId: String, risk: Double) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Acil durum bildirimi tasarımı
        val alertNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("🚨 KRİTİK YAPAY ZEKA UYARISI")
            .setContentText("$vehicleId aracında yüksek arıza riski tespit edildi! (%$risk)")
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Uyarı ikonu
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Ekrandan aşağı düşmesi için YÜKSEK öncelik
            .setAutoCancel(true)
            .build()

        // Her araç için farklı bir ID ile bildirim atıyoruz ki üst üste binmesinler
        notificationManager.notify(vehicleId.hashCode(), alertNotification)
    }

    override fun onDestroy() {
        super.onDestroy()
        socketService.closeClient() // Servis durursa tüneli kapat
    }
}