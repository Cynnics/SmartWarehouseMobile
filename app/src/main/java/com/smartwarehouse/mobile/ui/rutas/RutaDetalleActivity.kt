package com.smartwarehouse.mobile.ui.rutas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.showToast

class RutaDetalleActivity : AppCompatActivity(), OnMapReadyCallback {

    private val viewModel: RutaDetalleViewModel by viewModels()

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var tvIdRuta: TextView
    private lateinit var tvEstado: TextView
    private lateinit var tvDistancia: TextView
    private lateinit var tvDuracion: TextView
    private lateinit var tvNumeroPedidos: TextView
    private lateinit var btnIniciarRuta: Button
    private lateinit var btnCompletarRuta: Button
    private lateinit var btnNavegar: Button
    private lateinit var btnIniciarTracking: Button
    private lateinit var progressBar: ProgressBar

    private var idRuta: Int = -1
    private val markers = mutableListOf<Marker>()
    private var currentPolyline: Polyline? = null

    // Permission launcher
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                enableMyLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                enableMyLocation()
            }
            else -> {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ruta_detalle)

        idRuta = intent.getIntExtra("ID_RUTA", -1)
        if (idRuta == -1) {
            showToast("Error: ID de ruta inválido")
            finish()
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupToolbar()
        initializeViews()
        setupMap()
        setupObservers()
        setupListeners()

        viewModel.cargarRuta(idRuta)
        viewModel.cargarPedidosDeRuta(idRuta)
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Detalle de Ruta"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun initializeViews() {
        tvIdRuta = findViewById(R.id.tvIdRuta)
        tvEstado = findViewById(R.id.tvEstado)
        tvDistancia = findViewById(R.id.tvDistancia)
        tvDuracion = findViewById(R.id.tvDuracion)
        tvNumeroPedidos = findViewById(R.id.tvNumeroPedidos)
        btnIniciarRuta = findViewById(R.id.btnIniciarRuta)
        btnCompletarRuta = findViewById(R.id.btnCompletarRuta)
        btnNavegar = findViewById(R.id.btnNavegar)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Configurar estilo del mapa
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
        }

        // Solicitar permisos de ubicación
        checkLocationPermission()

        // Centrar el mapa en Madrid por defecto
        val madrid = LatLng(40.4168, -3.7038)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(madrid, 12f))
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true

            // Obtener ubicación actual
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))

                    // Enviar ubicación al servidor
                    viewModel.enviarUbicacionActual(it.latitude, it.longitude)
                }
            }
        }
    }

    private fun setupObservers() {
        // Observer de la ruta
        viewModel.ruta.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.let { ruta ->
                        tvIdRuta.text = "Ruta #${ruta.id}"
                        tvEstado.text = ruta.getEstadoTexto()
                        tvEstado.setTextColor(ruta.getEstadoColor())
                        tvDistancia.text = ruta.getDistanciaTexto()
                        tvDuracion.text = ruta.getDuracionTexto()

                        // Configurar visibilidad de botones según estado
                        when (ruta.estado) {
                            com.smartwarehouse.mobile.data.model.response.EstadoRuta.PENDIENTE -> {
                                btnIniciarRuta.visibility = View.VISIBLE
                                btnCompletarRuta.visibility = View.GONE
                                btnNavegar.visibility = View.GONE
                            }
                            com.smartwarehouse.mobile.data.model.response.EstadoRuta.EN_CURSO -> {
                                btnIniciarRuta.visibility = View.GONE
                                btnCompletarRuta.visibility = View.VISIBLE
                                btnNavegar.visibility = View.VISIBLE
                            }
                            com.smartwarehouse.mobile.data.model.response.EstadoRuta.COMPLETADA -> {
                                btnIniciarRuta.visibility = View.GONE
                                btnCompletarRuta.visibility = View.GONE
                                btnNavegar.visibility = View.GONE
                            }
                        }
                    }
                }
                is NetworkResult.Error -> {
                    showToast(result.message ?: "Error al cargar ruta")
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Observer de pedidos
        viewModel.pedidos.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    val pedidos = result.data ?: emptyList()
                    tvNumeroPedidos.text = "${pedidos.size} pedidos"

                    // Limpiar marcadores anteriores
                    markers.forEach { it.remove() }
                    markers.clear()

                    // TODO: Añadir marcadores en el mapa
                    // Por ahora, añadimos marcadores de ejemplo en Madrid
                    if (pedidos.isNotEmpty()) {
                        addSampleMarkers()
                    }
                }
                is NetworkResult.Error -> {
                    tvNumeroPedidos.text = "-- pedidos"
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Observer de cambio de estado
        viewModel.cambioEstadoResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    showToast("Estado actualizado correctamente")
                }
                is NetworkResult.Error -> {
                    showToast(result.message ?: "Error al cambiar estado")
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Observer de loading
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupListeners() {
        btnIniciarRuta.setOnClickListener {
            viewModel.iniciarRuta(idRuta)
        }

        btnCompletarRuta.setOnClickListener {
            viewModel.completarRuta(idRuta)
        }

        btnNavegar.setOnClickListener {
            abrirGoogleMapsNavegacion()
        }
    }

    // Marcadores de ejemplo (TODO: usar datos reales de pedidos)
    private fun addSampleMarkers() {
        val locations = listOf(
            LatLng(40.4168, -3.7038),
            LatLng(40.4200, -3.7100),
            LatLng(40.4150, -3.6900)
        )

        locations.forEachIndexed { index, location ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Pedido ${index + 1}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            marker?.let { markers.add(it) }
        }

        // Dibujar ruta entre los marcadores
        if (locations.size > 1) {
            drawRoute(locations)
        }

        // Ajustar cámara para mostrar todos los marcadores
        val builder = LatLngBounds.Builder()
        locations.forEach { builder.include(it) }
        val bounds = builder.build()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
    }

    private fun drawRoute(locations: List<LatLng>) {
        // Eliminar polilínea anterior
        currentPolyline?.remove()

        // Dibujar nueva polilínea
        val polylineOptions = PolylineOptions()
            .addAll(locations)
            .width(10f)
            .color(Color.BLUE)
            .geodesic(true)

        currentPolyline = map.addPolyline(polylineOptions)
    }

    private fun abrirGoogleMapsNavegacion() {
        // TODO: Usar coordenadas reales del primer pedido
        val destino = "40.4168,-3.7038" // Madrid (ejemplo)
        val uri = Uri.parse("google.navigation:q=$destino&mode=d")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            showToast("Google Maps no está instalado")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}