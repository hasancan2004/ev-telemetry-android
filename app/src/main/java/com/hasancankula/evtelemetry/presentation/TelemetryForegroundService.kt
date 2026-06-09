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
import kotlin.math.*

class TelemetryForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val socketService = TelemetrySocketService()
    private val channelId = "telemetry_alerts_channel"

    // Spam engelleme hafızaları
    private val alertedVehicles = mutableSetOf<String>()
    private val breachedVehicles = mutableSetOf<String>() // YENİ: Sınırı aşan araçların hafızası

    // ========================================================
    // YENİ: GÜVENLİ BÖLGE PARAMETRELERİ (Konya Merkez)
    // ========================================================
    private val centerLat = 37.8746
    private val centerLng = 32.4933
    private val maxDistanceMeters = 20000.0 // 20 Kilometre sınır

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Filo Takip Servisi Aktif")
            .setContentText("Yapay zeka ve Coğrafi Sınır (Geofence) koruması devrede...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
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
                    fleetList.forEach { vehicle ->

                        // 1. KONTROL: YAPAY ZEKA ARIZA RİSKİ (%75)
                        if (vehicle.maintenanceRiskPct > 75.0) {
                            if (!alertedVehicles.contains(vehicle.vehicleId)) {
                                sendCriticalAlert(vehicle.vehicleId, vehicle.maintenanceRiskPct)
                                alertedVehicles.add(vehicle.vehicleId)
                            }
                        } else {
                            alertedVehicles.remove(vehicle.vehicleId)
                        }

                        // ========================================================
                        // 2. KONTROL: COĞRAFİ SINIR İHLALİ (GEOFENCING)
                        // ========================================================
                        val distance = calculateDistance(vehicle.latitude, vehicle.longitude, centerLat, centerLng)

                        if (distance > maxDistanceMeters) {
                            // Araç 20 km dışına çıktıysa ve daha önce bildirim atılmadıysa alarm ver!
                            if (!breachedVehicles.contains(vehicle.vehicleId)) {
                                sendGeofenceAlert(vehicle.vehicleId, distance / 1000.0) // Metreyi KM'ye çevirip yolluyoruz
                                breachedVehicles.add(vehicle.vehicleId)
                            }
                        } else {
                            // Araç güvenli bölgeye geri döndüyse hafızadan sil
                            breachedVehicles.remove(vehicle.vehicleId)
                        }
                    }
                }
        }
    }

    // YENİ: İki koordinat arasındaki kuş uçuşu mesafeyi metre cinsinden ölçen matematiksel fonksiyon (Haversine Formula)
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Dünyanın yarıçapı (metre)
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val a = sin(deltaPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(deltaLambda / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c // Metre cinsinden mesafe
    }

    private fun sendCriticalAlert(vehicleId: String, risk: Double) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alertNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("🚨 KRİTİK YAPAY ZEKA UYARISI")
            .setContentText("$vehicleId aracında yüksek arıza riski tespit edildi! (%$risk)")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(vehicleId.hashCode(), alertNotification)
    }

    // YENİ: SINIR İHLALİ BİLDİRİM MOTORU
    private fun sendGeofenceAlert(vehicleId: String, distanceKm: Double) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val geofenceNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("⚠️ BÖLGE İHLALİ ALARMI")
            .setContentText("$vehicleId merkezden ${String.format("%.1f", distanceKm)} km uzaklaşarak GÜVENLİ ALANI TERK ETTİ!")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(1000, 1000, 1000)) // Telefonu titret
            .setAutoCancel(true)
            .build()

        // Arıza bildirimiyle çakışmasın diye benzersiz bir ID (Geofence için ayrı) veriyoruz
        notificationManager.notify((vehicleId + "_geofence").hashCode(), geofenceNotification)
    }

    override fun onDestroy() {
        super.onDestroy()
        socketService.closeClient()
    }
}