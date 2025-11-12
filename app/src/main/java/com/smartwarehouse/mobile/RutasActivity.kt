package com.smartwarehouse.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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

    private lateinit var googleMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rutas)

        // Inicializa el mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configura el RecyclerView
        val recycler = findViewById<RecyclerView>(R.id.recyclerRutas)
        recycler.layoutManager = LinearLayoutManager(this)

        val rutas = listOf(
            Ruta(1, "Juan Pérez", "Almacén Central", "Tienda Sur", "En ruta"),
            Ruta(2, "María López", "Almacén Norte", "Sucursal Centro", "Pendiente"),
            Ruta(3, "Carlos Ruiz", "Depósito Este", "Supermercado 12", "Completada")
        )

        recycler.adapter = RutaAdapter(rutas)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // 👉 Verifica permisos de ubicación
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Si el permiso ya está concedido, habilita la ubicación del usuario
            googleMap.isMyLocationEnabled = true
        } else {
            // Si no, solicita el permiso al usuario
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }

        // Simula la ubicación del almacén central
        val almacenCentral = LatLng(40.4168, -3.7038) // Madrid
        googleMap.addMarker(MarkerOptions().position(almacenCentral).title("Almacén Central"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(almacenCentral, 12f))

        // Múltiples ubicaciones simuladas
        val ubicaciones = listOf(
            LatLng(40.4168, -3.7038),  // Madrid
            LatLng(40.4379, -3.6793),  // Chamartín
            LatLng(40.4050, -3.7100)   // Usera
        )

        for ((index, ubicacion) in ubicaciones.withIndex()) {
            googleMap.addMarker(
                MarkerOptions()
                    .position(ubicacion)
                    .title("Repartidor ${index + 1}")
                    .snippet("Estado: En ruta")
            )
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                googleMap.isMyLocationEnabled = true
            }
        }
    }

}
