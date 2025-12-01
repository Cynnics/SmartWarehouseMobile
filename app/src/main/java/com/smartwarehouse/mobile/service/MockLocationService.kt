package com.smartwarehouse.mobile.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.maps.model.LatLng
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.data.repository.RutaRepository
import kotlinx.coroutines.*

/**
 * Servicio para simular movimiento GPS a lo largo de una ruta
 * Útil para pruebas sin necesidad de moverse físicamente
 */
class MockLocationService : Service() {

    private lateinit var rutaRepository: RutaRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var simulationJob: Job? = null

    private val NOTIFICATION_ID = 54321
    private val CHANNEL_ID = "mock_location_channel"
    private val UPDATE_INTERVAL = 5000L // 5 segundos para simulación

    companion object {
        var isMocking = false
            private set

        fun startMocking(context: Context) {
            val intent = Intent(context, MockLocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopMocking(context: Context) {
            val intent = Intent(context, MockLocationService::class.java)
            context.stopService(intent)
        }
    }

    // Ruta simulada (ejemplo: ruta por Madrid)
    private val mockRoute = listOf(
        LatLng(40.4168, -3.7038),  // Puerta del Sol
        LatLng(40.4200, -3.7050),  // Gran Vía
        LatLng(40.4230, -3.7100),  // Plaza España
        LatLng(40.4250, -3.7150),  // Templo de Debod
        LatLng(40.4280, -3.7180),  // Parque del Oeste
        LatLng(40.4300, -3.7200),  // Ciudad Universitaria
        LatLng(40.4320, -3.7220),  // Moncloa
        LatLng(40.4340, -3.7250)   // Valdezarza
    )

    private var currentIndex = 0

    override fun onCreate() {
        super.onCreate()
        rutaRepository = RutaRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startMockingLocations()
        isMocking = true
        return START_STICKY
    }

    private fun startMockingLocations() {
        simulationJob = serviceScope.launch {
            while (isActive) {
                val currentLocation = mockRoute[currentIndex]

                try {
                    rutaRepository.enviarUbicacion(
                        latitud = currentLocation.latitude,
                        longitud = currentLocation.longitude
                    )

                    android.util.Log.d("MockLocation",
                        "Ubicación simulada enviada: ${currentLocation.latitude}, ${currentLocation.longitude}")

                    updateNotification(currentLocation)
                } catch (e: Exception) {
                    android.util.Log.e("MockLocation", "Error al enviar ubicación simulada", e)
                }

                // Avanzar al siguiente punto
                currentIndex = (currentIndex + 1) % mockRoute.size

                delay(UPDATE_INTERVAL)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Simulación de Ubicación",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Simulación de GPS para pruebas"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(location: LatLng? = null): Notification {
        val notificationIntent = Intent(this, Class.forName("com.smartwarehouse.mobile.MainActivity"))
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val locationText = if (location != null) {
            "🎭 SIMULADO: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
        } else {
            "Iniciando simulación..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🧪 GPS Simulado (Pruebas)")
            .setContentText(locationText)
            .setSmallIcon(R.drawable.ic_user)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setColor(0xFFFF9800.toInt()) // Color naranja para diferenciarlo
            .build()
    }

    private fun updateNotification(location: LatLng) {
        val notification = createNotification(location)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        simulationJob?.cancel()
        serviceScope.cancel()
        isMocking = false
        android.util.Log.d("MockLocation", "Simulación de GPS detenida")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}