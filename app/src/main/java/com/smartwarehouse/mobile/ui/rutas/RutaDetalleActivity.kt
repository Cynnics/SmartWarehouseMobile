package com.smartwarehouse.mobile.ui.rutas

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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
import com.smartwarehouse.mobile.data.model.response.EstadoPedido
import com.smartwarehouse.mobile.data.model.response.Pedido
import com.smartwarehouse.mobile.service.MockLocationService
import com.smartwarehouse.mobile.ui.tracking.TrackingControlActivity
import com.smartwarehouse.mobile.utils.Constants
import com.smartwarehouse.mobile.utils.GeocodingHelper
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

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
        btnIniciarTracking = findViewById(R.id.btnIniciarTracking)
        progressBar = findViewById(R.id.progressBar)
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

        checkLocationPermission()

        val madrid = LatLng(40.4168, -3.7038)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(madrid, 12f))
    }

    private fun checkLocationPermission(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build   .VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 14f))

                    viewModel.enviarUbicacionActual(it.latitude, it.longitude)
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.ruta.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.let { ruta ->
                        tvIdRuta.text = "Ruta #${ruta.id}"
                        tvEstado.text = ruta.getEstadoTexto()
                        tvEstado.setTextColor(ruta.getEstadoColor())
                        tvDistancia.text = ruta.getDistanciaTexto()
                        tvDuracion.text = ruta.getDuracionTexto()

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

        viewModel.pedidos.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    val pedidos = result.data ?: emptyList()
                    tvNumeroPedidos.text = "${pedidos.size} pedidos"

                    if (pedidos.isNotEmpty()) {

                        ioScope.launch {
                            addPedidoMarkers(pedidos)
                        }
                    } else {
                        showToast("Esta ruta no tiene pedidos asignados")
                    }
                }
                is NetworkResult.Error -> {
                    tvNumeroPedidos.text = "-- pedidos"
                    showToast("Error al cargar pedidos de la ruta")
                }
                is NetworkResult.Loading -> {}
            }
        }


        viewModel.cambioEstadoResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    showToast("✅ Ruta actualizada correctamente")

                    viewModel.cargarRuta(idRuta)
                    viewModel.cargarPedidosDeRuta(idRuta)
                }
                is NetworkResult.Error -> {
                    showToast("❌ ${result.message}")
                }
                is NetworkResult.Loading -> {}
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupListeners() {
        btnIniciarRuta.setOnClickListener {
            confirmarIniciarRuta()
        }

        btnCompletarRuta.setOnClickListener {
            confirmarCompletarRuta()
        }

        btnNavegar.setOnClickListener {
            abrirGoogleMapsNavegacion()
        }
        findViewById<Button>(R.id.btnIniciarSimulacion).setOnClickListener {
            iniciarSimulacionConRutaReal()
        }
        btnIniciarTracking.setOnClickListener {
            startActivity(Intent(this, TrackingControlActivity::class.java))
        }

    }


    private fun confirmarIniciarRuta() {
        val numPedidos = (viewModel.pedidos.value as? NetworkResult.Success)?.data?.size ?: 0

        AlertDialog.Builder(this)
            .setTitle("Iniciar Ruta")
            .setMessage(
                "¿Iniciar esta ruta?\n\n" +
                        "• Todos los pedidos ($numPedidos) pasarán a estado 'En Reparto'\n" +
                        "• Se comenzará el tracking de la ruta\n\n" +
                        "¿Continuar?"
            )
            .setPositiveButton("Iniciar") { _, _ ->
                viewModel.iniciarRuta(idRuta)
                showToast("Iniciando ruta...")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarCompletarRuta() {
        val numPedidos = (viewModel.pedidos.value as? NetworkResult.Success)?.data?.size ?: 0

        AlertDialog.Builder(this)
            .setTitle("Completar Ruta")
            .setMessage(
                "¿Marcar esta ruta como completada?\n\n" +
                        "⚠️ IMPORTANTE:\n" +
                        "• Todos los pedidos ($numPedidos) pasarán a estado 'Entregado'\n" +
                        "• Esta acción NO se puede deshacer\n\n" +
                        "¿Estás seguro?"
            )
            .setPositiveButton("Completar") { _, _ ->
                viewModel.completarRuta(idRuta)
                showToast("Completando ruta...")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private suspend fun addPedidoMarkers(pedidos: List<Pedido>) {

        // Todo lo que toque Google Maps → hilo principal
        withContext(Dispatchers.Main) {
            // Limpiar marcadores anteriores
            markers.forEach { it.remove() }
            markers.clear()
            progressBar.visibility = View.VISIBLE
        }

        // Separar pedidos según tengan o no coordenadas
        val pedidosConCoords = pedidos.filter { it.tieneCoordenadasValidas() }
        val pedidosSinCoords = pedidos.filter { !it.tieneCoordenadasValidas() }

        // Geocodificar los que no tienen coordenadas (esto sí puede ir en IO)
        val pedidosGeocoded = if (pedidosSinCoords.isNotEmpty()) {
            geocodePedidos(pedidosSinCoords)
        } else emptyList()

        val pedidosConUbicacion = pedidosConCoords.map { PedidoConUbicacion(it, it.getLatLng()!!) } + pedidosGeocoded

        // Agregar marcadores y ajustar cámara → hilo principal
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.GONE

            if (pedidosConUbicacion.isEmpty()) {
                showToast("No se pudieron obtener las ubicaciones de los pedidos")
                return@withContext
            }

            pedidosConUbicacion.forEach { pedidoConUbicacion ->
                val pedido = pedidoConUbicacion.pedido
                val location = pedidoConUbicacion.coordenadas

                val markerColor = when (pedido.estado) {
                    EstadoPedido.PENDIENTE -> BitmapDescriptorFactory.HUE_YELLOW
                    EstadoPedido.PREPARADO -> BitmapDescriptorFactory.HUE_BLUE
                    EstadoPedido.EN_REPARTO -> BitmapDescriptorFactory.HUE_ORANGE
                    EstadoPedido.ENTREGADO -> BitmapDescriptorFactory.HUE_GREEN
                }

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(location)
                        .title("Pedido #${pedido.id}")
                        .snippet("${pedido.getEstadoTexto()} - ${pedido.getDireccionCompleta()}")
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                )

                marker?.let { markers.add(it) }
            }

            // Ajustar cámara
            if (markers.isNotEmpty()) {
                val builder = LatLngBounds.Builder()
                markers.forEach { builder.include(it.position) }
                currentLocationMarker?.let { builder.include(it.position) }
                val bounds = builder.build()
                val padding = 150
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            }

            // Calcular ruta optimizada → también Main
            calculateOptimizedRouteWithRealCoordinates(pedidosConUbicacion)
        }
    }

    private fun calculateOptimizedRouteWithRealCoordinates(pedidosConUbicacion: List<PedidoConUbicacion>) {
        if (pedidosConUbicacion.isEmpty()) return

        progressBar.visibility = View.VISIBLE

        ioScope.launch {
            try {
                // Obtener ubicación actual
                val origin = getCurrentLocation() ?: run {
                    withContext(Dispatchers.Main) {
                        showToast("No se pudo obtener tu ubicación actual")
                        progressBar.visibility = View.GONE
                    }
                    return@launch
                }

                val waypoints = pedidosConUbicacion.map { it.coordenadas }

                val route = getDirectionsRoute(origin, waypoints)

                withContext(Dispatchers.Main) {
                    route?.let {
                        drawRoute(it)
                        updateRouteInfo(it)
                        val distanciaKm = it.distanceMeters / 1000.0
                        val duracionMin = it.durationSeconds / 60
                        viewModel.guardarDistanciaYDuracion(idRuta, distanciaKm, duracionMin)

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

    private suspend fun getDirectionsRoute(origin: LatLng, waypoints: List<LatLng>): DirectionsRoute? {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = Constants.GOOGLE_MAPS_API_KEY

                val waypointsStr = waypoints.joinToString("|") { "${it.latitude},${it.longitude}" }

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

    private suspend fun drawRoute(route: DirectionsRoute) {
        withContext(Dispatchers.Main) {
            currentPolyline?.remove()

            val points = decodePolyline(route.polyline)

            val polylineOptions = PolylineOptions()
                .addAll(points)
                .width(10f)
                .color(Color.BLUE)
                .geodesic(true)

            currentPolyline = map.addPolyline(polylineOptions)
        }

    }

    private fun updateRouteInfo(route: DirectionsRoute) {
        val distanceKm = route.distanceMeters / 1000.0
        val durationMin = route.durationSeconds / 60

        tvDistancia.text = String.format("%.1f km", distanceKm)
        tvDuracion.text = "$durationMin min"
    }

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

    private fun abrirGoogleMapsNavegacion() {
        val pedidos = (viewModel.pedidos.value as? NetworkResult.Success)?.data
        if (pedidos.isNullOrEmpty()) {
            showToast("No hay pedidos en la ruta")
            return
        }

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

    private fun getLocationForPedido(index: Int): LatLng {
        val baseLocations = listOf(
            LatLng(40.4168, -3.7038),  // Puerta del Sol
            LatLng(40.4200, -3.7050),  // Gran Vía
            LatLng(40.4230, -3.7100),  // Plaza España
            LatLng(40.4250, -3.7150),  // Templo de Debod
            LatLng(40.4280, -3.7180),  // Parque del Oeste
        )
        return baseLocations[index % baseLocations.size]
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        ioScope.cancel()
    }

    private suspend fun geocodePedidos(pedidos: List<Pedido>): List<PedidoConUbicacion> {
        return withContext(Dispatchers.IO) {
            pedidos.mapNotNull { pedido ->
                if (!GeocodingHelper.isValidAddress(pedido.direccionEntrega)) {
                    Log.w("RutaDetalle", "Pedido ${pedido.id} sin dirección válida")
                    return@mapNotNull null
                }

                val direccionNormalizada = GeocodingHelper.normalizeAddress(
                    pedido.direccionEntrega ?: "",
                    ciudad = "Madrid",
                    pais = "España"
                )

                val coordinates = GeocodingHelper.getCoordinatesFromAddressWithCache(direccionNormalizada)

                if (coordinates != null) {
                    PedidoConUbicacion(
                        pedido = pedido,
                        coordenadas = coordinates
                    )
                } else {
                    Log.e("RutaDetalle", "No se pudo geocodificar: $direccionNormalizada")
                    null
                }
            }
        }
    }
    private fun iniciarSimulacionConRutaReal() {
        val pedidos = (viewModel.pedidos.value as? NetworkResult.Success)?.data

        if (pedidos.isNullOrEmpty()) {
            showToast("No hay pedidos en la ruta para simular")
            return
        }

        progressBar.visibility = View.VISIBLE

        ioScope.launch {
            try {
                val origin = getCurrentLocation() ?: run {
                    withContext(Dispatchers.Main) {
                        showToast("No se pudo obtener tu ubicación actual")
                        progressBar.visibility = View.GONE
                    }
                    return@launch
                }

                val pedidosConUbicacion = mutableListOf<PedidoConUbicacion>()

                for (pedido in pedidos) {
                    if (pedido.tieneCoordenadasValidas()) {
                        pedidosConUbicacion.add(
                            PedidoConUbicacion(pedido, pedido.getLatLng()!!)
                        )
                    } else {
                        val coords = GeocodingHelper.getCoordinatesFromAddressWithCache(
                            GeocodingHelper.normalizeAddress(pedido.direccionEntrega ?: "")
                        )
                        if (coords != null) {
                            pedidosConUbicacion.add(PedidoConUbicacion(pedido, coords))
                        }
                    }
                }

                if (pedidosConUbicacion.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        showToast("No se pudieron obtener las ubicaciones")
                        progressBar.visibility = View.GONE
                    }
                    return@launch
                }

                val waypoints = pedidosConUbicacion.map { it.coordenadas }
                val route = getDirectionsRoute(origin, waypoints)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (route != null) {
                        val routePoints = decodePolyline(route.polyline)

                        val interpolatedPoints = interpolateRoute(routePoints, 50)

                        MockLocationService.startMocking(this@RutaDetalleActivity, interpolatedPoints)

                        showToast("🧪 Simulación iniciada con ruta real (${interpolatedPoints.size} puntos)")

                        val intent = Intent(this@RutaDetalleActivity, TrackingControlActivity::class.java)
                        startActivity(intent)
                    } else {
                        showToast("No se pudo calcular la ruta")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("RutaDetalle", "Error al iniciar simulación", e)
                    showToast("Error: ${e.message}")
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun interpolateRoute(points: List<LatLng>, targetPoints: Int): List<LatLng> {
        if (points.size >= targetPoints) return points

        val result = mutableListOf<LatLng>()

        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]

            val steps = targetPoints / (points.size - 1)

            for (step in 0 until steps) {
                val fraction = step.toDouble() / steps
                val lat = start.latitude + (end.latitude - start.latitude) * fraction
                val lng = start.longitude + (end.longitude - start.longitude) * fraction
                result.add(LatLng(lat, lng))
            }
        }

        result.add(points.last())
        return result
    }
}

data class DirectionsRoute(
    val polyline: String,
    val distanceMeters: Int,
    val durationSeconds: Int
)

data class PedidoConUbicacion(
    val pedido: Pedido,
    val coordenadas: LatLng
)