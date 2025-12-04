package com.smartwarehouse.mobile.ui.catalogo

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.adapter.ProductoAdapter
import com.smartwarehouse.mobile.data.model.response.ProductoResponse
import com.smartwarehouse.mobile.ui.carrito.CarritoActivity
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.showToast

class CatalogoActivity : AppCompatActivity() {

    private val viewModel: CatalogoViewModel by viewModels()

    private lateinit var searchView: SearchView
    private lateinit var spinnerCategoria: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView

    private var menuInstance: Menu? = null // 🔥 IMPORTANTE: Guardar referencia del menú

    private val productoAdapter = ProductoAdapter(
        onProductoClick = { producto ->
            showToast("${producto.nombre} - ${producto.getPrecioFormateado()}")
        },
        onAgregarClick = { producto ->
            android.util.Log.d("CatalogoActivity", "Producto agregado: ${producto.nombre}")

            viewModel.agregarAlCarrito(producto)

            // 🔥 Verificar inmediatamente
            val itemsEnCarrito = viewModel.itemsEnCarrito.value ?: 0
            android.util.Log.d("CatalogoActivity", "Items en carrito: $itemsEnCarrito")

            showToast("${producto.nombre} añadido al carrito ($itemsEnCarrito)")
            invalidateOptionsMenu()
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalogo)


        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        initializeViews()
        setupRecyclerView()
        setupObservers()
        setupListeners()
    }


    private fun initializeViews() {
        searchView = findViewById(R.id.searchView)
        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        recyclerView = findViewById(R.id.recyclerProductos)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
    }

    private fun setupRecyclerView() {
        recyclerView.apply {
            layoutManager = GridLayoutManager(this@CatalogoActivity, 2)
            adapter = productoAdapter
        }
    }

    private fun setupObservers() {
        // Observer de productos filtrados
        viewModel.productosFiltrados.observe(this) { productos ->
            productoAdapter.submitList(productos)

            if (productos.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                emptyView.text = "No se encontraron productos"
            } else {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }

        // Observer de categorías
        viewModel.categorias.observe(this) { categorias ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategoria.adapter = adapter
        }

        // Observer del contador del carrito
        viewModel.itemsEnCarrito.observe(this) { cantidad ->
            invalidateOptionsMenu() // 🔥 Actualizar badge del carrito
        }

        // Observer de loading
        viewModel.isLoading.observe(this) { isLoading ->
            swipeRefresh.isRefreshing = isLoading
            progressBar.visibility = if (isLoading && productoAdapter.itemCount == 0) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        // Observer de productos (para errores)
        viewModel.productosFiltrados.observe(this) { productos: List<ProductoResponse> ->
            productoAdapter.submitList(productos)

            if (productos.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                emptyView.text = "No se encontraron productos"
            } else {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }

    }

    private fun setupListeners() {
        // Búsqueda
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.buscarProductos(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.buscarProductos(it) }
                return true
            }
        })

        // Filtro por categoría
        spinnerCategoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val categoria = parent?.getItemAtPosition(position).toString()
                viewModel.filtrarPorCategoria(categoria)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Swipe to refresh
        swipeRefresh.setOnRefreshListener {
            viewModel.cargarProductos()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_catalogo, menu)
        menuInstance = menu // 🔥 Guardar referencia

        // 🔥 Actualizar badge inicial
        actualizarBadgeCarrito(menu)

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // 🔥 Se llama cada vez que se invalida el menú
        actualizarBadgeCarrito(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    private fun actualizarBadgeCarrito(menu: Menu?) {
        menu?.findItem(R.id.action_carrito)?.let { menuItem ->
            val itemsEnCarrito = viewModel.itemsEnCarrito.value ?: 0

            if (itemsEnCarrito > 0) {
                menuItem.title = "🛒 ($itemsEnCarrito)"
            } else {
                menuItem.title = "🛒"
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_carrito -> {
                abrirCarrito()
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun abrirCarrito() {
        val intent = Intent(this, CarritoActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // 🔥 Actualizar contador cuando volvemos del carrito
        viewModel.actualizarContadorCarrito()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}