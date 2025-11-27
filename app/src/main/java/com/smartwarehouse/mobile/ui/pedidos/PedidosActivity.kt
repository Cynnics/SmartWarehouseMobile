package com.smartwarehouse.mobile.ui.pedidos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.ui.pedidos.adapter.PedidoAdapter
import com.smartwarehouse.mobile.domain.model.Pedido

class PedidosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedidos)

        val recycler = findViewById<RecyclerView>(R.id.recyclerPedidos)
        recycler.layoutManager = LinearLayoutManager(this)

        // Datos simulados
        val pedidos = listOf(
            Pedido(1, "Cliente A", "2025-11-11", "Pendiente"),
            Pedido(2, "Cliente B", "2025-11-10", "En camino"),
            Pedido(3, "Cliente C", "2025-11-09", "Entregado"),
            Pedido(4, "Cliente D", "2025-11-08", "Cancelado")
        )

        recycler.adapter = PedidoAdapter(pedidos)
    }
}