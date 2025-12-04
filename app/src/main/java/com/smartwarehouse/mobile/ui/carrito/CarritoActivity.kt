package com.smartwarehouse.mobile.ui.carrito

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.adapter.CarritoAdapter
import com.smartwarehouse.mobile.ui.pedidos.PedidosActivity
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.showToast
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch


class CarritoActivity : AppCompatActivity() {

    private val viewModel: CarritoViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvIva: TextView
    private lateinit var tvTotal: TextView
    private lateinit var etDireccion: EditText
    private lateinit var etNotas: EditText
    private lateinit var btnConfirmarPedido: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    private lateinit var etCiudad: EditText
    private lateinit var etCodigoPostal: EditText
    private lateinit var btnCrearPedido: MaterialButton


    private val carritoAdapter = CarritoAdapter(
        onIncrementar = { idProducto ->
            viewModel.incrementarCantidad(idProducto)
        },
        onDecrementar = { idProducto ->
            viewModel.decrementarCantidad(idProducto)
        },
        onEliminar = { idProducto ->
            confirmarEliminar(idProducto)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carrito)

        setupToolbar()
        initializeViews()
        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Mi Carrito"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerProductos)   // RecyclerView del carrito
        tvSubtotal = findViewById(R.id.tvSubtotal)
        tvIva = findViewById(R.id.tvIva)
        tvTotal = findViewById(R.id.tvTotal)
        etDireccion = findViewById(R.id.etDireccion)
        etCiudad = findViewById(R.id.etCiudad)
        etCodigoPostal = findViewById(R.id.etCodigoPostal)
        etNotas = findViewById(R.id.etNotas)
        btnConfirmarPedido = findViewById(R.id.btnCrearPedido)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.tvCarritoVacio)        // Mensaje "carrito vacío"
        btnCrearPedido = findViewById(R.id.btnCrearPedido)
    }


    private fun setupRecyclerView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CarritoActivity)
            adapter = carritoAdapter
        }
    }

    private fun setupObservers() {
        viewModel.items.observe(this) { items ->
            carritoAdapter.submitList(items)

            if (items.isEmpty()) {
                mostrarVistaVacia()
            } else {
                mostrarVistaConItems()
            }
        }

        viewModel.subtotal.observe(this) { subtotal ->
            tvSubtotal.text = String.format("%.2f €", subtotal)
        }

        viewModel.iva.observe(this) { iva ->
            tvIva.text = String.format("%.2f €", iva)
        }

        viewModel.total.observe(this) { total ->
            tvTotal.text = String.format("%.2f €", total)
        }

        viewModel.crearPedidoResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    mostrarDialogoExito()
                }
                is NetworkResult.Error -> {
                    showToast(result.message ?: "Error al crear el pedido")
                }
                is NetworkResult.Loading -> {}
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnConfirmarPedido.isEnabled = !isLoading
        }
    }

    private fun setupListeners() {
        btnConfirmarPedido.setOnClickListener {
            confirmarPedido()
        }
    }

    private fun mostrarVistaVacia() {
        emptyView.visibility = View.VISIBLE        // Muestra mensaje "carrito vacío"
        recyclerView.visibility = View.GONE
        tvSubtotal.visibility = View.GONE
        tvIva.visibility = View.GONE
        tvTotal.visibility = View.GONE
        btnConfirmarPedido.visibility = View.GONE
    }

    private fun mostrarVistaConItems() {
        emptyView.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        tvSubtotal.visibility = View.VISIBLE
        tvIva.visibility = View.VISIBLE
        tvTotal.visibility = View.VISIBLE
        btnConfirmarPedido.visibility = View.VISIBLE
    }


    private fun confirmarEliminar(idProducto: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Deseas eliminar este producto del carrito?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarItem(idProducto)
                showToast("Producto eliminado")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarPedido() {
        val direccion = etDireccion.text.toString().trim()
        val ciudad = etCiudad.text.toString().trim()
        val codigoPostal = etCodigoPostal.text.toString().trim()
        val notas = etNotas.text.toString().trim().ifBlank { null }

        // Validaciones
        if (direccion.isBlank()) { showToast("La dirección es obligatoria"); return }
        if (ciudad.isBlank()) { showToast("La ciudad es obligatoria"); return }
        if (codigoPostal.isBlank()) { showToast("El código postal es obligatorio"); return }
        if (viewModel.carrito.isEmpty()) { showToast("El carrito está vacío"); return }


        AlertDialog.Builder(this)
            .setTitle("Confirmar Pedido")
            .setMessage("¿Deseas crear el pedido con la dirección ingresada?")
            .setPositiveButton("Sí") { _, _ ->
                btnCrearPedido.isEnabled = false
                lifecycleScope.launch {
                    try {
                        val (lat, lng) = viewModel.calcularCoordenadasSuspend(
                            direccion, ciudad, codigoPostal
                        )
                        viewModel.setDireccion(direccion)
                        viewModel.setCiudad(ciudad)
                        viewModel.setCodigoPostal(codigoPostal)
                        viewModel.setNotas(notas)
                        viewModel.setLatitud(lat)
                        viewModel.setLongitud(lng)
                        Log.d("Pedido", "Creando pedido con direccion: '$direccion'")
                        // Crear pedido
                        viewModel.crearPedido()

                    } finally {
                        btnCrearPedido.isEnabled = true // reactivar botón

                    }

                }
            }
                    .setNegativeButton("No", null)
                    .show()

    }


    private fun mostrarDialogoExito() {
        AlertDialog.Builder(this)
            .setTitle("✅ Pedido Creado")
            .setMessage("Tu pedido ha sido creado con éxito.\n\nPronto recibirás confirmación y podrás hacer seguimiento del mismo.")
            .setPositiveButton("Ver Mis Pedidos") { _, _ ->
                val intent = Intent(this, PedidosActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Volver al Catálogo") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (!viewModel.carritoEstaVacio()) {
            menuInflater.inflate(R.menu.menu_carrito, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_vaciar -> {
                confirmarVaciarCarrito()
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmarVaciarCarrito() {
        AlertDialog.Builder(this)
            .setTitle("Vaciar Carrito")
            .setMessage("¿Deseas vaciar todo el carrito?")
            .setPositiveButton("Vaciar") { _, _ ->
                viewModel.vaciarCarrito()
                showToast("Carrito vaciado")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        viewModel.actualizarCarrito()
    }
}