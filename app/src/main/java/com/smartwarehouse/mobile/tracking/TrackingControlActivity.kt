package com.smartwarehouse.mobile.tracking

import android.Manifest
import android.content.pm.PackageManager
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
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                showToast("Permisos de ubicación concedidos")
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                showToast("Permiso de ubicación aproximada concedido")
            }
            else -> {
                showToast("Permisos de ubicación denegados")
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
        updateUI()
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
            if (checkLocationPermission()) {
                if (switchModoSimulacion.isChecked) {
                    // Iniciar simulación
                    MockLocationService.Companion.startMocking(this)
                    showToast("🧪 Simulación GPS iniciada")
                } else {
                    // Iniciar tracking real
                    LocationTrackingService.Companion.startTracking(this)
                    showToast("📍 Tracking GPS iniciado")
                }
                updateUI()
            }
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

    private fun checkLocationPermission(): Boolean {
        return when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                true
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
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
        tvEstadoTracking.text = if (LocationTrackingService.Companion.isTracking) {
            "✅ ACTIVO - Enviando ubicación real"
        } else {
            "❌ INACTIVO"
        }

        // Estado de la simulación
        tvEstadoMock.text = if (MockLocationService.Companion.isMocking) {
            "✅ ACTIVO - Enviando ubicación simulada"
        } else {
            "❌ INACTIVO"
        }

        // Botones
        val isAnyServiceActive = LocationTrackingService.Companion.isTracking || MockLocationService.Companion.isMocking
        btnIniciarTracking.isEnabled = !isAnyServiceActive
        btnDetenerTracking.isEnabled = isAnyServiceActive
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