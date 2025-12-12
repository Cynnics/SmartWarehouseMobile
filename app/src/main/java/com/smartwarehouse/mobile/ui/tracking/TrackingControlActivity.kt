package com.smartwarehouse.mobile.ui.tracking

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.service.LocationTrackingService
import com.smartwarehouse.mobile.service.MockLocationService
import com.smartwarehouse.mobile.utils.showToast

class TrackingControlActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap

    private lateinit var tvEstadoTracking: TextView
    private lateinit var tvEstadoMock: TextView
    private lateinit var tvUltimaUbicacion: TextView
    private lateinit var tvActualizaciones: TextView
    private lateinit var btnIniciarTracking: Button
    private lateinit var btnDetenerTracking: Button
    private lateinit var switchModoSimulacion: Switch

    private var currentMarker: Marker? = null
    private var polyline: Polyline? = null
    private val routePoints = mutableListOf<LatLng>()
    private var updateCount = 0

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val lng = intent?.getDoubleExtra("longitude", 0.0) ?: 0.0
            val isMock = intent?.getBooleanExtra("isMock", false) ?: false

            onLocationUpdate(lat, lng, isMock)
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
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
        setupMap()
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
        tvActualizaciones = findViewById(R.id.tvActualizaciones)
        btnIniciarTracking = findViewById(R.id.btnIniciarTracking)
        btnDetenerTracking = findViewById(R.id.btnDetenerTracking)
        switchModoSimulacion = findViewById(R.id.switchModoSimulacion)
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
        }

        val madrid = LatLng(40.4168, -3.7038)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(madrid, 13f))

        enableMyLocation()
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        }
    }

    private fun setupListeners() {
        btnIniciarTracking.setOnClickListener {
            if (!canStartTracking()) {
                showToast("⚠️ Se necesitan permisos de ubicación")
                checkLocationPermission()
                return@setOnClickListener
            }

            val anyActive = LocationTrackingService.isTracking || MockLocationService.isMocking
            if (anyActive) {
                showToast("El tracking ya está activo")
                return@setOnClickListener
            }

            clearMap()

            if (switchModoSimulacion.isChecked) {
                MockLocationService.startMocking(this)
                showToast("🧪 Simulación GPS iniciada")
            } else {
                LocationTrackingService.startTracking(this)
                showToast("📍 Tracking GPS iniciado")
            }

            btnIniciarTracking.postDelayed({
                updateUI()
            }, 500)
        }

        btnDetenerTracking.setOnClickListener {
            if (MockLocationService.isMocking) {
                MockLocationService.stopMocking(this)
                showToast("🧪 Simulación GPS detenida")
            }
            if (LocationTrackingService.isTracking) {
                LocationTrackingService.stopTracking(this)
                showToast("📍 Tracking GPS detenido")
            }
            updateUI()
        }

        switchModoSimulacion.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showToast("Modo SIMULACIÓN activado")
            } else {
                showToast("Modo REAL activado")
            }
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
            true
        }

        return fineLocation && foregroundService
    }

    private fun checkLocationPermission(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted) {
            locationPermissionRequest.launch(permissions.toTypedArray())
            return false
        }

        return true
    }

    private fun updateUI() {
        tvEstadoTracking.text = if (LocationTrackingService.isTracking) {
            "✅ ACTIVO - Enviando ubicación real cada 30s"
        } else {
            "❌ INACTIVO"
        }

        tvEstadoMock.text = if (MockLocationService.isMocking) {
            "✅ ACTIVO - Enviando ubicación simulada cada 5s"
        } else {
            "❌ INACTIVO"
        }

        val isAnyServiceActive = LocationTrackingService.isTracking || MockLocationService.isMocking

        btnIniciarTracking.isEnabled = !isAnyServiceActive
        btnDetenerTracking.isEnabled = isAnyServiceActive

        switchModoSimulacion.isChecked = MockLocationService.isMocking
        switchModoSimulacion.isEnabled = !isAnyServiceActive
    }

    /**
     * Actualiza el mapa con nueva ubicación
     */
    private fun onLocationUpdate(lat: Double, lng: Double, isMock: Boolean) {
        updateCount++
        tvActualizaciones.text = "Actualizaciones: $updateCount"

        val location = LatLng(lat, lng)

        val locationText = """
            ${if (isMock) "🧪 SIMULADO" else "📍 REAL"}
            Lat: ${String.format("%.6f", lat)}
            Lng: ${String.format("%.6f", lng)}
            Actualizaciones: $updateCount
        """.trimIndent()
        tvUltimaUbicacion.text = locationText

        routePoints.add(location)

        if (currentMarker == null) {
            currentMarker = map.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(if (isMock) "Simulación GPS" else "Mi Ubicación")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                        if (isMock) BitmapDescriptorFactory.HUE_ORANGE
                        else BitmapDescriptorFactory.HUE_BLUE
                    ))
            )
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        } else {
            currentMarker?.position = location
            map.animateCamera(CameraUpdateFactory.newLatLng(location))
        }

        updatePolyline()
    }

    private fun updatePolyline() {
        polyline?.remove()

        if (routePoints.size > 1) {
            val polylineOptions = PolylineOptions()
                .addAll(routePoints)
                .width(8f)
                .color(Color.BLUE)
                .geodesic(true)

            polyline = map.addPolyline(polylineOptions)
        }
    }

    private fun clearMap() {
        currentMarker?.remove()
        currentMarker = null
        polyline?.remove()
        polyline = null
        routePoints.clear()
        updateCount = 0
        tvActualizaciones.text = "Actualizaciones: 0"
        tvUltimaUbicacion.text = "Esperando actualización..."
    }

    override fun onResume() {
        super.onResume()

        val filter = IntentFilter("LOCATION_UPDATE")
        LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver, filter)

        updateUI()
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}