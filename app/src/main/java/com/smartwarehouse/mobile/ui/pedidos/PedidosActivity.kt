package com.smartwarehouse.mobile.ui.pedidos

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.adapter.PedidoAdapter
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.showToast

class PedidosActivity : AppCompatActivity() {

    private val viewModel: PedidosViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var spinnerFiltro: Spinner

    private val pedidoAdapter = PedidoAdapter { pedido ->
        abrirDetallePedido(pedido.id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedidos)

        setupToolbar()
        initializeViews()
        setupRecyclerView()
        setupFiltros()
        setupObservers()
        setupSwipeRefresh()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Mis Pedidos"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerPedidos)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
        spinnerFiltro = findViewById(R.id.spinnerFiltro)
    }

    private fun setupRecyclerView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@PedidosActivity)
            adapter = pedidoAdapter
        }
    }

    private fun setupFiltros() {
        val estados = arrayOf(
            "Todos",
            "Pendiente",
            "Preparado",
            "En Reparto",
            "Entregado"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, estados)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFiltro.adapter = adapter

        spinnerFiltro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val estadoSeleccionado = when (position) {
                    0 -> "todos"
                    1 -> "pendiente"
                    2 -> "preparado"
                    3 -> "en_reparto"
                    4 -> "entregado"
                    else -> "todos"
                }
                viewModel.filtrarPorEstado(estadoSeleccionado)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupObservers() {
        viewModel.pedidos.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    val pedidos = result.data ?: emptyList()
                    pedidoAdapter.submitList(pedidos)

                    if (pedidos.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        emptyView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }
                }
                is NetworkResult.Error -> {
                    showToast(result.message ?: "Error al cargar pedidos")
                    emptyView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
                is NetworkResult.Loading -> {
                    // El estado de carga se maneja en isLoading
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            swipeRefresh.isRefreshing = isLoading
            progressBar.visibility = if (isLoading && pedidoAdapter.itemCount == 0) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            viewModel.cargarPedidos()
        }
    }

    private fun abrirDetallePedido(idPedido: Int) {
        val intent = Intent(this, PedidoDetalleActivity::class.java)
        intent.putExtra("ID_PEDIDO", idPedido)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        // Recargar pedidos al volver (por si cambió algún estado)
        viewModel.cargarPedidos()
    }
}