package com.smartwarehouse.mobile.ui.rutas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.smartwarehouse.mobile.data.model.response.Pedido
import com.smartwarehouse.mobile.utils.Constants
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.showToast
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject


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
    private var currentLocationMarker: Marker? = null

    // Coroutine scope para llamadas asíncronas
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())


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

                    if (pedidos.isNotEmpty()){
                        // Limpiar marcadores anteriores
                        markers.forEach { it.remove() }
                        markers.clear()

                        // Añadir marcadores reales de pedidos
                        addPedidoMarkers(pedidos)

                        // Calcular y dibujar ruta óptima
                        calculateOptimizedRoute(pedidos)
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
    /**
     * Añade marcadores en el mapa para cada pedido
     */
    private fun addPedidoMarkers(pedidos: List<Pedido>) {
        pedidos.forEachIndexed { index, pedido ->
            // TODO: Obtener coordenadas reales desde Geocoding API
            // Por ahora usamos coordenadas de ejemplo en Madrid
            val location = getLocationForPedido(index)

            val markerColor = when (pedido.estado) {
                com.smartwarehouse.mobile.data.model.response.EstadoPedido.PENDIENTE ->
                    BitmapDescriptorFactory.HUE_YELLOW
                com.smartwarehouse.mobile.data.model.response.EstadoPedido.PREPARADO ->
                    BitmapDescriptorFactory.HUE_BLUE
                com.smartwarehouse.mobile.data.model.response.EstadoPedido.EN_REPARTO ->
                    BitmapDescriptorFactory.HUE_ORANGE
                com.smartwarehouse.mobile.data.model.response.EstadoPedido.ENTREGADO ->
                    BitmapDescriptorFactory.HUE_GREEN
            }

            val marker = map.addMarker(
                MarkerOptions()
                    .position(location)
                    .title("Pedido #${pedido.id}")
                    .snippet("${pedido.getEstadoTexto()} - ${pedido.nombreCliente ?: "Cliente"}")
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
            )
            marker?.let { markers.add(it) }
        }

        // Ajustar cámara para mostrar todos los marcadores
        if (markers.isNotEmpty()) {
            val builder = LatLngBounds.Builder()
            markers.forEach { builder.include(it.position) }

            // Incluir ubicación actual si existe
            currentLocationMarker?.let { builder.include(it.position) }

            val bounds = builder.build()
            val padding = 150 // padding en pixels
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        }
    }

    /**
     * Calcula la ruta optimizada usando Directions API
     */
    private fun calculateOptimizedRoute(pedidos: List<Pedido>) {
        if (pedidos.isEmpty()) return

        progressBar.visibility = View.VISIBLE

        ioScope.launch {
            try {
                // Obtener ubicación actual
                val origin = getCurrentLocation() ?: return@launch

                // Obtener coordenadas de todos los pedidos
                val waypoints = pedidos.mapIndexed { index, _ ->
                    getLocationForPedido(index)
                }

                // Llamar a Directions API
                val route = getDirectionsRoute(origin, waypoints)

                withContext(Dispatchers.Main) {
                    route?.let {
                        drawRoute(it)
                        updateRouteInfo(it)
                    } ?: showToast("No se pudo calcular la ruta")

                    progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("RutaDetalle", "Error al calcular ruta", e)
                    showToast("Error al calcular ruta: ${e.message}")
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Obtiene la ubicación actual del dispositivo de forma síncrona
     */
    private suspend fun getCurrentLocation(): LatLng? {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                if (ContextCompat.checkSelfPermission(
                        this@RutaDetalleActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            continuation.resume(LatLng(location.latitude, location.longitude)) {}
                        } else {
                            continuation.resume(null) {}
                        }
                    }.addOnFailureListener {
                        continuation.resume(null) {}
                    }
                } else {
                    continuation.resume(null) {}
                }
            }
        }
    }

    /**
     * Llama a Google Directions API para obtener la ruta
     */
    private suspend fun getDirectionsRoute(origin: LatLng, waypoints: List<LatLng>): DirectionsRoute? {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = Constants.GOOGLE_MAPS_API_KEY

                // Construir waypoints string
                val waypointsStr = waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }

                // URL de Directions API
                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${origin.latitude},${origin.longitude}" +
                        "&destination=${waypoints.last().latitude},${waypoints.last().longitude}" +
                        "&waypoints=optimize:true|$waypointsStr" +
                        "&key=$apiKey"

                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")

                    if (json.getString("status") == "OK") {
                        val routes = json.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val route = routes.getJSONObject(0)
                            val polyline = route.getJSONObject("overview_polyline").getString("points")
                            val legs = route.getJSONArray("legs")

                            // Calcular distancia y duración total
                            var totalDistance = 0
                            var totalDuration = 0
                            for (i in 0 until legs.length()) {
                                val leg = legs.getJSONObject(i)
                                totalDistance += leg.getJSONObject("distance").getInt("value")
                                totalDuration += leg.getJSONObject("duration").getInt("value")
                            }

                            return@withContext DirectionsRoute(
                                polyline = polyline,
                                distanceMeters = totalDistance,
                                durationSeconds = totalDuration
                            )
                        }
                    } else {
                        Log.e("DirectionsAPI", "Status: ${json.getString("status")}")
                    }
                }

                null
            } catch (e: Exception) {
                Log.e("DirectionsAPI", "Error", e)
                null
            }
        }
    }

    /**
     * Dibuja la ruta en el mapa
     */

    private fun drawRoute(route: DirectionsRoute) {
        // Eliminar polilínea anterior
        currentPolyline?.remove()

        // Decodificar polyline
        val points = decodePolyline(route.polyline)

        // Dibujar nueva polilínea
        val polylineOptions = PolylineOptions()
            .addAll(points)
            .width(10f)
            .color(Color.BLUE)
            .geodesic(true)

        currentPolyline = map.addPolyline(polylineOptions)
    }

    /**
     * Actualiza la información de distancia y duración
     */
    private fun updateRouteInfo(route: DirectionsRoute) {
        val distanceKm = route.distanceMeters / 1000.0
        val durationMin = route.durationSeconds / 60

        tvDistancia.text = String.format("%.1f km", distanceKm)
        tvDuracion.text = "$durationMin min"
    }

    /**
     * Decodifica un polyline de Google Maps
     */
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(latLng)
        }

        return poly
    }

    /**
     * Obtiene coordenadas de ejemplo para un pedido
     * TODO: Reemplazar con Geocoding API real
     */
    private fun getLocationForPedido(index: Int): LatLng {
        // Coordenadas de ejemplo en Madrid
        val baseLocations = listOf(
            LatLng(40.4168, -3.7038),  // Puerta del Sol
            LatLng(40.4200, -3.7050),  // Gran Vía
            LatLng(40.4230, -3.7100),  // Plaza España
            LatLng(40.4250, -3.7150),  // Templo de Debod
            LatLng(40.4280, -3.7180),  // Parque del Oeste
        )
        return baseLocations[index % baseLocations.size]
    }

    private fun abrirGoogleMapsNavegacion() {
        val pedidos = (viewModel.pedidos.value as? NetworkResult.Success)?.data
        if (pedidos.isNullOrEmpty()) {
            showToast("No hay pedidos en la ruta")
            return
        }

        // Obtener primer pedido
        val primerPedido = getLocationForPedido(0)
        val destino = "${primerPedido.latitude},${primerPedido.longitude}"

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

    override fun onDestroy() {
        super.onDestroy()
        ioScope.cancel()
    }
}

/**
 * Data class para la ruta de Directions API
 */
data class DirectionsRoute(
    val polyline: String,
    val distanceMeters: Int,
    val durationSeconds: Int
)