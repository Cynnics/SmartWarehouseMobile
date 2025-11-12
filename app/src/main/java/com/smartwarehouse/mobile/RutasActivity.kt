package com.smartwarehouse.mobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.smartwarehouse.R
import com.smartwarehouse.mobile.adapter.RutaAdapter
import com.smartwarehouse.mobile.model.Ruta

class RutasActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rutas)

        // Inicializar el mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configurar lista de rutas
        val recycler = findViewById<RecyclerView>(R.id.recyclerRutas)
        recycler.layoutManager = LinearLayoutManager(this)

        val rutas = listOf(
            Ruta(1, "Juan Pérez", "Almacén Central", "Tienda Sur", "En ruta"),
            Ruta(2, "María López", "Almacén Norte", "Sucursal Centro", "Pendiente"),
            Ruta(3, "Carlos Ruiz", "Depósito Este", "Supermercado 12", "Completada")
        )

        recycler.adapter = RutaAdapter(rutas)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Punto de ejemplo (Madrid)
        val madrid = LatLng(40.4168, -3.7038)
        mMap.addMarker(MarkerOptions().position(madrid).title("Centro logístico"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(madrid, 12f))
    }
}
