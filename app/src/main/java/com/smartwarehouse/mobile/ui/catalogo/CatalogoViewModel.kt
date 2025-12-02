package com.smartwarehouse.mobile.ui.catalogo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.model.response.ProductoResponse
import com.smartwarehouse.mobile.data.repository.ProductoRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.launch

class CatalogoViewModel(application: Application) : AndroidViewModel(application) {

    private val productoRepository = ProductoRepository(application)

    private val _productos = MutableLiveData<NetworkResult<List<ProductoResponse>>>()
    val productos: LiveData<NetworkResult<List<ProductoResponse>>> = _productos

    private val _productosFiltrados = MutableLiveData<List<ProductoResponse>>()
    val productosFiltrados: LiveData<List<ProductoResponse>> = _productosFiltrados

    private val _categorias = MutableLiveData<List<String>>()
    val categorias: LiveData<List<String>> = _categorias

    private val _itemsEnCarrito = MutableLiveData<Int>()
    val itemsEnCarrito: LiveData<Int> = _itemsEnCarrito

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private var todosLosProductos: List<ProductoResponse> = emptyList()
    private var categoriaSeleccionada: String? = null
    private var queryBusqueda: String = ""

    init {
        cargarProductos()
        actualizarContadorCarrito()
    }

    fun cargarProductos() {
        _isLoading.value = true
        _productos.value = NetworkResult.Loading()

        viewModelScope.launch {
            val result = productoRepository.getProductos()
            _productos.value = result

            if (result is NetworkResult.Success) {
                todosLosProductos = result.data ?: emptyList()
                _productosFiltrados.value = todosLosProductos

                // Extraer categorías
                val categoriasList = mutableListOf("Todas")
                categoriasList.addAll(productoRepository.getCategorias(todosLosProductos))
                _categorias.value = categoriasList
            }

            _isLoading.value = false
        }
    }

    fun buscarProductos(query: String) {
        queryBusqueda = query
        aplicarFiltros()
    }

    fun filtrarPorCategoria(categoria: String) {
        categoriaSeleccionada = if (categoria == "Todas") null else categoria
        aplicarFiltros()
    }

    private fun aplicarFiltros() {
        var productosFiltrados = todosLosProductos

        // Aplicar filtro de categoría
        productosFiltrados = productoRepository.filtrarPorCategoria(
            categoriaSeleccionada,
            productosFiltrados
        )

        // Aplicar búsqueda
        productosFiltrados = productoRepository.buscarProductos(
            queryBusqueda,
            productosFiltrados
        )

        _productosFiltrados.value = productosFiltrados
    }

    fun agregarAlCarrito(producto: ProductoResponse) {
        android.util.Log.d("CatalogoViewModel", "Antes de agregar: ${ProductoRepository.carrito.getTotalItems()}")

        ProductoRepository.carrito.agregarProducto(producto)

        android.util.Log.d("CatalogoViewModel", "Después de agregar: ${ProductoRepository.carrito.getTotalItems()}")

        actualizarContadorCarrito()
    }

    fun actualizarContadorCarrito() {
        val total = ProductoRepository.carrito.getTotalItems()
        android.util.Log.d("CatalogoViewModel", "Actualizando contador a: $total")
        _itemsEnCarrito.value = total
    }

    fun getCarrito() = ProductoRepository.carrito
}