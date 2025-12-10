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

    private fun checkLocationPermission(): Boolean {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        // Añadir permiso de foreground service si es necesario
        if (Build   .VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }

        // Añadir permiso de notificaciones si es necesario
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

                    // 🔥 LOG PARA DEBUG
                    pedidos.forEach {
                        Log.d("RutaDetalle", "Pedido #${it.id} Dirección: '${it.direccionEntrega}' " +
                                "Lat: ${it.latitud}, Lng: ${it.longitud}")
                    }

                    tvNumeroPedidos.text = "${pedidos.size} pedidos"

                    if (pedidos.isNotEmpty()) {
                        // Lanzar coroutine para añadir marcadores
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
        findViewById<Button>(R.id.btnIniciarSimulacion).setOnClickListener {
            iniciarSimulacionConRutaReal()
        }
        btnIniciarTracking.setOnClickListener {
            startActivity(Intent(this, TrackingControlActivity::class.java))
        }

    }
    /**
     * Añade marcadores en el mapa para cada pedido
     */
    private suspend fun addPedidoMarkers(pedidos: List<Pedido>) {
        // Limpiar marcadores anteriores
        markers.forEach { it.remove() }
        markers.clear()

        // Mostrar loading
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
        }

        // Separar pedidos según tengan o no coordenadas
        val pedidosConCoords = pedidos.filter { it.tieneCoordenadasValidas() }
        val pedidosSinCoords = pedidos.filter { !it.tieneCoordenadasValidas() }

        // 🔥 LOG DE DEBUG
        Log.d("RutaDetalle", "Pedidos con coords: ${pedidosConCoords.size}")
        Log.d("RutaDetalle", "Pedidos sin coords: ${pedidosSinCoords.size}")

        // Geocodificar los que no tienen coordenadas
        val pedidosGeocoded = if (pedidosSinCoords.isNotEmpty()) {
            geocodePedidos(pedidosSinCoords)
        } else {
            emptyList()
        }

        // Combinar todos los pedidos con ubicación
        val pedidosConUbicacion = pedidosConCoords.map { pedido ->
            PedidoConUbicacion(
                pedido = pedido,
                coordenadas = pedido.getLatLng()!! // Sabemos que existe
            )
        } + pedidosGeocoded

        // Añadir marcadores al mapa
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

            // Calcular ruta óptima
            calculateOptimizedRouteWithRealCoordinates(pedidosConUbicacion)
        }
    }

    /**
     * Calcula la ruta optimizada usando Directions API
     */
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

                // Extraer coordenadas de los pedidos
                val waypoints = pedidosConUbicacion.map { it.coordenadas }

                // Llamar a Directions API
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
                // Validar que tenga dirección
                if (!GeocodingHelper.isValidAddress(pedido.direccionEntrega)) {
                    Log.w("RutaDetalle", "Pedido ${pedido.id} sin dirección válida")
                    return@mapNotNull null
                }

                // Normalizar dirección (añadir ciudad/país si falta)
                val direccionNormalizada = GeocodingHelper.normalizeAddress(
                    pedido.direccionEntrega ?: "",
                    ciudad = "Madrid",
                    pais = "España"
                )

                // Geocodificar con cache
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
                // 1. Obtener ubicación actual
                val origin = getCurrentLocation() ?: run {
                    withContext(Dispatchers.Main) {
                        showToast("No se pudo obtener tu ubicación actual")
                        progressBar.visibility = View.GONE
                    }
                    return@launch
                }

                // 2. Geocodificar pedidos si es necesario
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

                // 3. Obtener ruta de Directions API
                val waypoints = pedidosConUbicacion.map { it.coordenadas }
                val route = getDirectionsRoute(origin, waypoints)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (route != null) {
                        // 4. Decodificar polyline en puntos
                        val routePoints = decodePolyline(route.polyline)

                        // 5. Interpolar puntos (para que la simulación sea más suave)
                        val interpolatedPoints = interpolateRoute(routePoints, 50)

                        // 6. Iniciar simulación con estos puntos
                        MockLocationService.startMocking(this@RutaDetalleActivity, interpolatedPoints)

                        showToast("🧪 Simulación iniciada con ruta real (${interpolatedPoints.size} puntos)")

                        // 7. Abrir TrackingControlActivity
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

    /**
     * Interpola puntos en una ruta para simulación más suave
     */
    private fun interpolateRoute(points: List<LatLng>, targetPoints: Int): List<LatLng> {
        if (points.size >= targetPoints) return points

        val result = mutableListOf<LatLng>()

        for (i in 0 until points.size - 1) {
            val start = points[i]
            val end = points[i + 1]

            // Calcular cuántos puntos interpolar entre estos dos
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
private suspend fun guardarCoordenadasEnCache(pedidoId: Int, coordenadas: LatLng) {
    // TODO: Implementar si quieres cache persistente
    // Por ahora GeocodingHelper.geocodingCache hace el trabajo
}



/**
 * Data class para la ruta de Directions API
 */
data class DirectionsRoute(
    val polyline: String,
    val distanceMeters: Int,
    val durationSeconds: Int
)

data class PedidoConUbicacion(
    val pedido: Pedido,
    val coordenadas: LatLng
)