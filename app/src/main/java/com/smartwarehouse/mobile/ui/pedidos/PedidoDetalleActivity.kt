package com.smartwarehouse.mobile.ui.pedidos

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.adapter.DetallePedidoAdapter
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.showToast
import com.smartwarehouse.mobile.utils.toDate
import com.smartwarehouse.mobile.utils.toFormattedString

class PedidoDetalleActivity : AppCompatActivity() {

    private val viewModel: PedidoDetalleViewModel by viewModels()

    private lateinit var tvIdPedido: TextView
    private lateinit var tvEstado: TextView
    private lateinit var tvFechaPedido: TextView
    private lateinit var tvFechaEntrega: TextView
    private lateinit var recyclerDetalles: RecyclerView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvIva: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnCambiarEstado: Button
    private lateinit var progressBar: ProgressBar

    private val detalleAdapter = DetallePedidoAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedido_detalle)

        val idPedido = intent.getIntExtra("ID_PEDIDO", -1)
        if (idPedido == -1) {
            showToast("Error: ID de pedido inválido")
            finish()
            return
        }

        setupToolbar()
        initializeViews()
        setupRecyclerView()
        setupObservers()

        viewModel.cargarPedido(idPedido)
        viewModel.cargarDetalles(idPedido)
        viewModel.cargarTotales(idPedido)
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Detalle del Pedido"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun initializeViews() {
        tvIdPedido = findViewById(R.id.tvIdPedido)
        tvEstado = findViewById(R.id.tvEstado)
        tvFechaPedido = findViewById(R.id.tvFechaPedido)
        tvFechaEntrega = findViewById(R.id.tvFechaEntrega)
        recyclerDetalles = findViewById(R.id.recyclerDetalles)
        tvSubtotal = findViewById(R.id.tvSubtotal)
        tvIva = findViewById(R.id.tvIva)
        tvTotal = findViewById(R.id.tvTotal)
        btnCambiarEstado = findViewById(R.id.btnCambiarEstado)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        recyclerDetalles.apply {
            layoutManager = LinearLayoutManager(this@PedidoDetalleActivity)
            adapter = detalleAdapter
        }
    }

    private fun setupObservers() {
        // Observer del pedido
        viewModel.pedido.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.let { pedido ->
                        tvIdPedido.text = "Pedido #${pedido.id}"
                        tvEstado.text = pedido.getEstadoTexto()
                        tvEstado.setTextColor(pedido.getEstadoColor())

                        val fecha = pedido.fechaPedido.toDate()?.toFormattedString()
                            ?: pedido.fechaPedido
                        tvFechaPedido.text = "Fecha: $fecha"

                        if (pedido.fechaEntrega != null) {
                            val fechaEntrega = pedido.fechaEntrega.toDate()?.toFormattedString()
                                ?: pedido.fechaEntrega
                            tvFechaEntrega.text = "Entregado: $fechaEntrega"
                            tvFechaEntrega.visibility = View.VISIBLE
                        } else {
                            tvFechaEntrega.visibility = View.GONE
                        }

                        // Mostrar botón de cambiar estado solo si es repartidor y el pedido no está entregado
                        btnCambiarEstado.visibility = if (viewModel.esRepartidor() && pedido.estado.name != "ENTREGADO") {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                }
                is NetworkResult.Error -> {
                    showToast(result.message ?: "Error al cargar pedido")
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Observer de detalles
        viewModel.detalles.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    detalleAdapter.submitList(result.data ?: emptyList())
                }
                is NetworkResult.Error -> {
                    showToast("Error al cargar detalles")
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Observer de totales
        viewModel.totales.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.let { totales ->
                        tvSubtotal.text = String.format("%.2f €", totales.subtotal)
                        tvIva.text = String.format("%.2f €", totales.iva)
                        tvTotal.text = String.format("%.2f €", totales.total)
                    }
                }
                is NetworkResult.Error -> {
                    tvSubtotal.text = "-- €"
                    tvIva.text = "-- €"
                    tvTotal.text = "-- €"
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Observer de cambio de estado
        viewModel.cambioEstadoResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    showToast("Estado actualizado correctamente")
                    // Recargar el pedido
                    val idPedido = intent.getIntExtra("ID_PEDIDO", -1)
                    viewModel.cargarPedido(idPedido)
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

        // Click del botón cambiar estado
        btnCambiarEstado.setOnClickListener {
            mostrarDialogoCambiarEstado()
        }
    }

    private fun mostrarDialogoCambiarEstado() {
        val estados = arrayOf("Pendiente", "Preparado", "En Reparto", "Entregado")

        AlertDialog.Builder(this)
            .setTitle("Cambiar Estado")
            .setItems(estados) { _, which ->
                val nuevoEstado = when (which) {
                    0 -> "pendiente"
                    1 -> "preparado"
                    2 -> "en_reparto"
                    3 -> "entregado"
                    else -> return@setItems
                }

                val idPedido = intent.getIntExtra("ID_PEDIDO", -1)
                viewModel.cambiarEstado(idPedido, nuevoEstado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}