package com.smartwarehouse.mobile

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import com.smartwarehouse.R
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RutaDetalleActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val TAG = "RutaDetalle"

    private lateinit var tvRepartidor: TextView
    private lateinit var tvOrigen: TextView
    private lateinit var tvDestino: TextView
    private lateinit var tvEstado: TextView
    private lateinit var btnCompletar: Button

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ruta_detalle)

        tvRepartidor = findViewById(R.id.tvRepartidorDetalle)
        tvOrigen = findViewById(R.id.tvOrigenDetalle)
        tvDestino = findViewById(R.id.tvDestinoDetalle)
        tvEstado = findViewById(R.id.tvEstadoDetalle)
        btnCompletar = findViewById(R.id.btnCompletar)

        val repartidor = intent.getStringExtra("repartidor") ?: "Desconocido"
        val origenTxt = intent.getStringExtra("origen") ?: "Sin origen"
        val destinoTxt = intent.getStringExtra("destino") ?: "Sin destino"
        val estado = intent.getStringExtra("estado") ?: "Pendiente"

        tvRepartidor.text = "Repartidor: $repartidor"
        tvOrigen.text = "Origen: $origenTxt"
        tvDestino.text = "Destino: $destinoTxt"
        tvEstado.text = "Estado: $estado"

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.detalleMapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        btnCompletar.setOnClickListener {
            tvEstado.text = "Estado: Completada ✅"
            btnCompletar.isEnabled = false
            btnCompletar.text = "Entrega finalizada"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Log.d(TAG, "Mapa listo.")
        map = googleMap

        val origenTxt = intent.getStringExtra("origen") ?: ""
        val destinoTxt = intent.getStringExtra("destino") ?: ""

        val ubicaciones = mapOf(
            "Almacén Central" to LatLng(40.4168, -3.7038),
            "Almacén Norte" to LatLng(40.45, -3.70),
            "Depósito Este" to LatLng(40.42, -3.68),
            "Tienda Sur" to LatLng(40.38, -3.72),
            "Sucursal Centro" to LatLng(40.415, -3.70),
            "Supermercado 12" to LatLng(40.40, -3.69)
        )

        val origen = ubicaciones[origenTxt] ?: LatLng(40.4168, -3.7038)
        val destino = ubicaciones[destinoTxt] ?: LatLng(40.4, -3.7)

        map.addMarker(MarkerOptions().position(origen).title("Origen: $origenTxt"))
        map.addMarker(MarkerOptions().position(destino).title("Destino: $destinoTxt"))

        val middle = LatLng(
            (origen.latitude + destino.latitude) / 2,
            (origen.longitude + destino.longitude) / 2
        )
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(middle, 12f))

        map.setOnMapLoadedCallback {
            Log.d(TAG, "Mapa cargado completamente. Dibujando ruta (retraso 400ms)...")
            // Pequeño retraso para evitar que el render se congele
            mapHandler.postDelayed({
                dibujarRuta(origen, destino)
            }, 400)
        }
    }

    private val mapHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private fun dibujarRuta(origen: LatLng, destino: LatLng) {
        val apiKey = "AIzaSyD9ooqhxBFDRbQbsXqLBY5neUspRBV3W-8"
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origen.latitude},${origen.longitude}" +
                "&destination=${destino.latitude},${destino.longitude}" +
                "&key=$apiKey"

        Log.d(TAG, "URL Directions: $url")

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        uiScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Ejecutando petición HTTP...")
                val response = client.newCall(Request.Builder().url(url).build()).execute()
                Log.d(TAG, "HTTP code: ${response.code}")

                val body = response.body?.string()
                response.close()

                if (body.isNullOrEmpty()) {
                    Log.e(TAG, "Respuesta vacía del servidor")
                    return@launch
                }

                val json = JSONObject(body)
                val routes = json.optJSONArray("routes")

                if (routes == null || routes.length() == 0) {
                    Log.e(TAG, "Sin rutas encontradas.")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RutaDetalleActivity, "No se encontró una ruta válida", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val points = routes.getJSONObject(0)
                    .getJSONObject("overview_polyline")
                    .getString("points")

                val decodedPath = PolyUtil.decode(points)
                Log.d(TAG, "Ruta decodificada correctamente: ${decodedPath.size} puntos")

                withContext(Dispatchers.Main) {
                    if (::map.isInitialized) {
                        try {
                            map.addPolyline(
                                PolylineOptions()
                                    .addAll(decodedPath)
                                    .color(Color.BLUE)
                                    .width(8f)
                            )
                            Log.d(TAG, "Ruta dibujada correctamente.")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error dibujando la ruta", e)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener la ruta", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RutaDetalleActivity, "Error al obtener la ruta", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
