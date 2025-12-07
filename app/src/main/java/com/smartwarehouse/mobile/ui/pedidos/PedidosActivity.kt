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

class PedidosActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TODOS_PEDIDOS = "EXTRA_TODOS_PEDIDOS"
    }

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

        val mostrarTodos = intent.getBooleanExtra(EXTRA_TODOS_PEDIDOS, false)
        supportActionBar?.title = if (mostrarTodos) "Todos los Pedidos" else "Mis Pedidos"
        viewModel.sincronizarPedidos(mostrarTodos)
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
        // 🔥 Observer de pedidos (desde Room)
        viewModel.pedidos.observe(this) { pedidos ->
            android.util.Log.d("PedidosActivity", "Pedidos recibidos: ${pedidos.size}")

            pedidoAdapter.submitList(pedidos)

            if (pedidos.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                emptyView.text = "No hay pedidos disponibles"
            } else {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }

        // 🔥 Observer de loading
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
            viewModel.sincronizarPedidos()
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
        viewModel.sincronizarPedidos()
    }
}