package com.smartwarehouse.mobile.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.data.repository.RutaRepository
import com.smartwarehouse.mobile.ui.main.MainActivity
import com.smartwarehouse.mobile.utils.SessionManager
import kotlinx.coroutines.*

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var rutaRepository: RutaRepository
    private lateinit var sessionManager: SessionManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Configuración del tracking
    private val UPDATE_INTERVAL = 30000L // 30 segundos
    private val FASTEST_INTERVAL = 15000L // 15 segundos (mínimo)
    private val NOTIFICATION_ID = 12345
    private val CHANNEL_ID = "location_tracking_channel"

    companion object {
        var isTracking = false
            private set

        fun startTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopTracking(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        rutaRepository = RutaRepository(applicationContext)
        sessionManager = SessionManager.getInstance(applicationContext)

        createNotificationChannel()
        setupLocationCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
        isTracking = true
        return START_STICKY
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    sendLocationToServer(location)
                    updateNotification(location)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        val locationRequest = LocationRequest.create().apply {
            interval = UPDATE_INTERVAL
            fastestInterval = FASTEST_INTERVAL
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun sendLocationToServer(location: Location) {
        serviceScope.launch {
            try {
                rutaRepository.enviarUbicacion(
                    latitud = location.latitude,
                    longitud = location.longitude
                )
                // Log exitoso (opcional)
                android.util.Log.d("LocationTracking",
                    "Ubicación enviada: ${location.latitude}, ${location.longitude}")
            } catch (e: Exception) {
                android.util.Log.e("LocationTracking", "Error al enviar ubicación", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Seguimiento de Ubicación",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación de seguimiento GPS en tiempo real"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(location: Location? = null): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val locationText = if (location != null) {
            "Ubicación: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
        } else {
            "Obteniendo ubicación..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚚 Seguimiento Activo")
            .setContentText(locationText)
            .setSmallIcon(R.drawable.ic_user) // Cambia por tu icono
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(location: Location) {
        val notification = createNotification(location)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        isTracking = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}