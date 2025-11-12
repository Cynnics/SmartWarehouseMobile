package com.smartwarehouse.mobile

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.smartwarehouse.R

class RutaDetalleActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap

    private lateinit var tvRepartidor: TextView
    private lateinit var tvOrigen: TextView
    private lateinit var tvDestino: TextView
    private lateinit var tvEstado: TextView
    private lateinit var btnCompletar: Button

    private var origen: LatLng = LatLng(40.4168, -3.7038)  // Madrid (por defecto)
    private var destino: LatLng = LatLng(40.4, -3.7)       // Simulado

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ruta_detalle)

        // Referencias UI
        tvRepartidor = findViewById(R.id.tvRepartidorDetalle)
        tvOrigen = findViewById(R.id.tvOrigenDetalle)
        tvDestino = findViewById(R.id.tvDestinoDetalle)
        tvEstado = findViewById(R.id.tvEstadoDetalle)
        btnCompletar = findViewById(R.id.btnCompletar)

        // Recibe los datos del intent
        val idRuta = intent.getIntExtra("idRuta", -1)
        val repartidor = intent.getStringExtra("repartidor") ?: "Desconocido"
        val origenTxt = intent.getStringExtra("origen") ?: "Sin origen"
        val destinoTxt = intent.getStringExtra("destino") ?: "Sin destino"
        val estado = intent.getStringExtra("estado") ?: "Pendiente"

        // Asigna datos al layout
        tvRepartidor.text = "Repartidor: $repartidor"
        tvOrigen.text = "Origen: $origenTxt"
        tvDestino.text = "Destino: $destinoTxt"
        tvEstado.text = "Estado: $estado"

        // Configura el mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.detalleMapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Acción del botón
        btnCompletar.setOnClickListener {
            tvEstado.text = "Estado: Completada ✅"
            btnCompletar.isEnabled = false
            btnCompletar.text = "Entrega finalizada"
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Recogemos los textos que llegaron por Intent
        val origenTxt = intent.getStringExtra("origen") ?: ""
        val destinoTxt = intent.getStringExtra("destino") ?: ""

        // Coordenadas simuladas para las ubicaciones conocidas
        val ubicaciones = mapOf(
            "Almacén Central" to LatLng(40.4168, -3.7038),   // Madrid
            "Almacén Norte" to LatLng(40.45, -3.70),
            "Depósito Este" to LatLng(40.42, -3.68),
            "Tienda Sur" to LatLng(40.38, -3.72),
            "Sucursal Centro" to LatLng(40.415, -3.70),
            "Supermercado 12" to LatLng(40.40, -3.69)
        )

        val origen = ubicaciones[origenTxt] ?: LatLng(40.4168, -3.7038)
        val destino = ubicaciones[destinoTxt] ?: LatLng(40.4, -3.7)

        // Añadimos los marcadores en el mapa
        map.addMarker(MarkerOptions().position(origen).title("Origen: $origenTxt"))
        map.addMarker(MarkerOptions().position(destino).title("Destino: $destinoTxt"))

        // Centramos la cámara entre los dos puntos
        val middleLat = (origen.latitude + destino.latitude) / 2
        val middleLng = (origen.longitude + destino.longitude) / 2
        val middle = LatLng(middleLat, middleLng)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(middle, 12f))
    }

}
