package com.smartwarehouse.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rutas)

        // Mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // RecyclerView
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

        // Permisos de ubicación
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = false // ✅ NO activar aún (espera a carga completa)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
        }

        // Centrar cámara
        val almacenCentral = LatLng(40.4168, -3.7038)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(almacenCentral, 11f))

        // Esperar a que el mapa termine de renderizar
        googleMap.setOnMapLoadedCallback {
            handler.postDelayed({
                agregarMarcadores()
            }, 100)
        }
    }

    private fun agregarMarcadores() {
        val ubicaciones = listOf(
            LatLng(40.4168, -3.7038),  // Madrid
            LatLng(40.4379, -3.6793),  // Chamartín
            LatLng(40.4050, -3.7100)   // Usera
        )

        ubicaciones.forEachIndexed { index, ubicacion ->
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

        if (requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                googleMap.isMyLocationEnabled = true
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // ✅ Evita que el mapa quede renderizando al salir
        handler.removeCallbacksAndMessages(null)
    }
}
