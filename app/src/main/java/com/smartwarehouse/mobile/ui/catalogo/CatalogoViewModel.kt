package com.smartwarehouse.mobile.ui.catalogo

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.model.response.ProductoResponse
import com.smartwarehouse.mobile.data.repository.ProductoRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CatalogoViewModel(application: Application) : AndroidViewModel(application) {
    private val productoRepository = ProductoRepository(application)

    private val _categorias = MutableLiveData<List<String>>()
    val categorias: LiveData<List<String>> = _categorias

    private val _itemsEnCarrito = MutableLiveData<Int>()
    val itemsEnCarrito: LiveData<Int> = _itemsEnCarrito

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _categoriaSeleccionada = MutableStateFlow("Todas")
    private val _queryBusqueda = MutableStateFlow("")

    val productosFiltrados: LiveData<List<ProductoResponse>> =
        combine(
            productoRepository.getProductos(),
            _categoriaSeleccionada,
            _queryBusqueda
        ) { productos, categoria, query ->
            var filtrados = productos
            if (categoria != "Todas") filtrados = productoRepository.filtrarPorCategoria(categoria, filtrados)
            if (query.isNotBlank()) filtrados = productoRepository.buscarProductos(query, filtrados)
            filtrados
        }.asLiveData(viewModelScope.coroutineContext)

    init {
        cargarProductos()
        actualizarContadorCarrito()
    }

    fun cargarProductos() {
        viewModelScope.launch {
            _isLoading.value = true

            if (productoRepository.needsSync()) {
                productoRepository.syncProductos()
            } else {
                launch { productoRepository.syncProductos() }
            }

            val primeraCarga = productoRepository.getProductos().first()
            val categoriasUnicas = productoRepository.getCategorias(primeraCarga)
            _categorias.value = listOf("Todas") + categoriasUnicas

            _isLoading.value = false

            launch {
                productoRepository.getProductos().collect { productos ->
                    _categorias.value = listOf("Todas") + productoRepository.getCategorias(productos)
                }
            }
        }
    }

    fun filtrarPorCategoria(categoria: String) {
        _categoriaSeleccionada.value = categoria
    }

    fun buscarProductos(query: String) {
        _queryBusqueda.value = query
    }

    fun agregarProductoAlCarrito(producto: ProductoResponse) {
        if (producto.stock <= 0 || !producto.activo) return

        val itemEnCarrito = ProductoRepository.carrito.items
            .find { it.producto.idProducto == producto.idProducto }

        if (itemEnCarrito != null && itemEnCarrito.cantidad >= producto.stock) return

        ProductoRepository.carrito.agregarProducto(producto)
        actualizarContadorCarrito()
    }

    fun actualizarContadorCarrito() {
        _itemsEnCarrito.value = ProductoRepository.carrito.getTotalItems()
    }
}