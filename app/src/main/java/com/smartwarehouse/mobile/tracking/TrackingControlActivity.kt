package com.smartwarehouse.mobile.tracking

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.service.LocationTrackingService
import com.smartwarehouse.mobile.service.MockLocationService
import com.smartwarehouse.mobile.utils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackingControlActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var tvEstadoTracking: TextView
    private lateinit var tvEstadoMock: TextView
    private lateinit var tvUltimaUbicacion: TextView
    private lateinit var btnIniciarTracking: Button
    private lateinit var btnDetenerTracking: Button
    private lateinit var switchModoSimulacion: Switch
    private lateinit var btnObtenerUbicacion: Button

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val foregroundService = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions[Manifest.permission.FOREGROUND_SERVICE_LOCATION] ?: false
        } else {
            true
        }

        when {
            fineLocation && foregroundService -> {
                showToast("✅ Permisos concedidos")
                updateUI()
            }
            coarseLocation -> {
                showToast("⚠️ Solo ubicación aproximada concedida")
                updateUI()
            }
            else -> {
                showToast("❌ Permisos de ubicación denegados")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking_control)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupToolbar()
        initializeViews()
        setupListeners()
        checkLocationPermission()
        updateUIInitial()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Control de Tracking GPS"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun initializeViews() {
        tvEstadoTracking = findViewById(R.id.tvEstadoTracking)
        tvEstadoMock = findViewById(R.id.tvEstadoMock)
        tvUltimaUbicacion = findViewById(R.id.tvUltimaUbicacion)
        btnIniciarTracking = findViewById(R.id.btnIniciarTracking)
        btnDetenerTracking = findViewById(R.id.btnDetenerTracking)
        switchModoSimulacion = findViewById(R.id.switchModoSimulacion)
        btnObtenerUbicacion = findViewById(R.id.btnObtenerUbicacion)
    }

    private fun setupListeners() {

        btnIniciarTracking.setOnClickListener {
            if (!canStartTracking()) {
                showToast("Se necesitan permisos de ubicación y Foreground Service")
                return@setOnClickListener
            }

            // Verificar que no haya tracking activo
            val anyActive = LocationTrackingService.isTracking || MockLocationService.isMocking
            if (anyActive) {
                showToast("El tracking ya está activo")
                return@setOnClickListener
            }

            // Iniciar servicio según modo
            if (switchModoSimulacion.isChecked) {
                MockLocationService.startMocking(this)
                showToast("🧪 Simulación GPS iniciada")
            } else {
                LocationTrackingService.startTracking(this)
                showToast("📍 Tracking GPS iniciado")
            }

            // Actualizar UI después de un pequeño delay
            btnIniciarTracking.postDelayed({
                updateUI()
            }, 500)
        }

        btnDetenerTracking.setOnClickListener {
            if (MockLocationService.Companion.isMocking) {
                MockLocationService.Companion.stopMocking(this)
                showToast("🧪 Simulación GPS detenida")
            }
            if (LocationTrackingService.Companion.isTracking) {
                LocationTrackingService.Companion.stopTracking(this)
                showToast("📍 Tracking GPS detenido")
            }
            updateUI()
        }

        switchModoSimulacion.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showToast("Modo SIMULACIÓN activado (para pruebas)")
            } else {
                showToast("Modo REAL activado")
            }
        }

        btnObtenerUbicacion.setOnClickListener {
            obtenerUbicacionActual()
        }
    }

    private fun canStartTracking(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val foregroundService = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // No se requiere en versiones anteriores
        }

        if (!fineLocation || !foregroundService) {
            showToast("⚠️ Faltan permisos necesarios")
            checkLocationPermission() // Solicitar permisos
            return false
        }

        return true
    }


    private fun checkLocationPermission(): Boolean {
        return when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                true
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.FOREGROUND_SERVICE_LOCATION
                    )
                )
                false
            }
        }
    }

    private fun obtenerUbicacionActual() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val locationText = """
                        Latitud: ${String.format("%.6f", location.latitude)}
                        Longitud: ${String.format("%.6f", location.longitude)}
                        Precisión: ${location.accuracy}m
                    """.trimIndent()
                    tvUltimaUbicacion.text = locationText
                    showToast("Ubicación obtenida")
                } else {
                    tvUltimaUbicacion.text = "No se pudo obtener la ubicación"
                    showToast("Error al obtener ubicación")
                }
            }
        } else {
            showToast("Permisos de ubicación no concedidos")
        }
    }

    private fun updateUI() {
        // Estado del tracking real
        tvEstadoTracking.text = if (LocationTrackingService.isTracking) {
            "✅ ACTIVO - Enviando ubicación real"
        } else {
            "❌ INACTIVO"
        }

        // Estado de la simulación
        tvEstadoMock.text = if (MockLocationService.isMocking) {
            "✅ ACTIVO - Enviando ubicación simulada"
        } else {
            "❌ INACTIVO"
        }

        // Habilitar/deshabilitar botones
        val isAnyServiceActive = LocationTrackingService.isTracking || MockLocationService.isMocking

        btnIniciarTracking.isEnabled = !isAnyServiceActive
        btnDetenerTracking.isEnabled = isAnyServiceActive

        // Deshabilitar switch mientras hay tracking activo
        switchModoSimulacion.isEnabled = !isAnyServiceActive
    }

    private fun updateUIInitial() {
        tvEstadoTracking.text = "Desconocido"
        tvEstadoMock.text = "Desconocido"
        tvUltimaUbicacion.text = "Desconocida"
    }


    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}