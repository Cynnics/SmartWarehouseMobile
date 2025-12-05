package com.smartwarehouse.mobile.ui.rutas

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.adapter.PedidoSeleccionableAdapter
import com.smartwarehouse.mobile.data.local.entity.PedidoEntity
import com.smartwarehouse.mobile.data.local.mappers.toDomain
import com.smartwarehouse.mobile.data.local.mappers.toEntity
import com.smartwarehouse.mobile.data.model.response.Pedido

class AsignarRutaActivity : AppCompatActivity() {

    private lateinit var recyclerPedidos: RecyclerView
    private lateinit var spinnerRepartidores: Spinner
    private lateinit var btnCrearRuta: MaterialButton
    private lateinit var emptyView: TextView

    // Pedidos seleccionados
    private val selectedPedidos = mutableSetOf<Pedido>()

    private val pedidoAdapter = PedidoSeleccionableAdapter { pedido, isChecked ->
        if (isChecked) selectedPedidos.add(pedido)
        else selectedPedidos.remove(pedido)
    }

    private val viewModel: AsignarRutaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asignar_rutas)

        recyclerPedidos = findViewById(R.id.recyclerPedidos)
        spinnerRepartidores = findViewById(R.id.spinnerRepartidores)
        btnCrearRuta = findViewById(R.id.btnCrearRuta)
        emptyView = findViewById(R.id.emptyView)

        setupRecycler()
        setupObservers()
        setupButton()
    }

    private fun setupRecycler() {
        recyclerPedidos.apply {
            layoutManager = LinearLayoutManager(this@AsignarRutaActivity)
            adapter = pedidoAdapter
        }
    }

    private fun setupObservers() {

        // Pedidos pendientes desde ROOM → Domain
        viewModel.pedidosPendientes.observe(this) { list ->
            val domainList = list.map { it.toDomain() }
            pedidoAdapter.submitList(domainList)
            emptyView.visibility = if (domainList.isEmpty()) View.VISIBLE else View.GONE
        }

        // Repartidores
        viewModel.repartidores.observe(this) { lista ->
            val nombres = lista.map { it.nombre }
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombres)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerRepartidores.adapter = adapter
        }
    }

    private fun setupButton() {
        btnCrearRuta.setOnClickListener {

            if (selectedPedidos.isEmpty()) {
                Toast.makeText(this, "Selecciona al menos un pedido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pos = spinnerRepartidores.selectedItemPosition
            val repartidor = viewModel.repartidores.value?.getOrNull(pos)

            if (repartidor == null) {
                Toast.makeText(this, "Selecciona un repartidor", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convertir Domain → Entity (Room)
            val pedidosEntity = selectedPedidos.map { it.toEntity() }

            viewModel.crearRuta(
                idRepartidor = repartidor.idUsuario,
                pedidosSeleccionados = pedidosEntity
            ) { ok, msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

                if (ok) {
                    selectedPedidos.clear()
                }
            }
        }
    }
}
