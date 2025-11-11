package com.smartwarehouse.mobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartwarehouse.R
import com.smartwarehouse.mobile.adapter.RutaAdapter
import com.smartwarehouse.mobile.model.Ruta

class RutasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rutas)

        val recycler = findViewById<RecyclerView>(R.id.recyclerRutas)
        recycler.layoutManager = LinearLayoutManager(this)

        val rutas = listOf(
            Ruta(1, "Juan Pérez", "Almacén Central", "Tienda Sur", "En ruta"),
            Ruta(2, "María López", "Almacén Norte", "Sucursal Centro", "Pendiente"),
            Ruta(3, "Carlos Ruiz", "Depósito Este", "Supermercado 12", "Completada")
        )

        recycler.adapter = RutaAdapter(rutas)
    }
}
