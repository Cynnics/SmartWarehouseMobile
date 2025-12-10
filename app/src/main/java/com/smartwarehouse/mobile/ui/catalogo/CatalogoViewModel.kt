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

    private val _todosLosProductos = MutableLiveData<List<ProductoResponse>>()

    private val _categorias = MutableLiveData<List<String>>()
    val categorias: LiveData<List<String>> = _categorias

    private val _itemsEnCarrito = MutableLiveData<Int>()
    val itemsEnCarrito: LiveData<Int> = _itemsEnCarrito

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _syncResult = MutableLiveData<NetworkResult<Boolean>>()
    val syncResult: LiveData<NetworkResult<Boolean>> = _syncResult

    // ✅ Mensaje para mostrar al usuario cuando intenta agregar sin stock
    private val _mensajeUsuario = MutableLiveData<String?>()
    val mensajeUsuario: LiveData<String?> = _mensajeUsuario

    private val _categoriaSeleccionada = MutableStateFlow("Todas")
    val categoriaSeleccionada = _categoriaSeleccionada.asStateFlow()

    private val _queryBusqueda = MutableStateFlow("")
    val queryBusqueda = _queryBusqueda.asStateFlow()

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
            Log.d("CatalogoViewModel", "Cargando productos...")

            // 🔍 Sincronización
            if (productoRepository.needsSync()) {
                val result = productoRepository.syncProductos()
                _syncResult.value = result
            } else {
                launch { productoRepository.syncProductos() }
            }
            // 🔥 Tomar solo la PRIMERA carga del Flow (evita loading infinito)
            val primeraCarga = productoRepository.getProductos().first()
            Log.d("CatalogoViewModel", "Primeros productos recibidos: ${primeraCarga.size}")

            _todosLosProductos.value = primeraCarga

            // Categorías iniciales
            val categoriasUnicas = productoRepository.getCategorias(primeraCarga)
            _categorias.value = listOf("Todas") + categoriasUnicas

            _isLoading.value = false  // 🔥 Ahora sí se ejecuta

            // 🔥 Si quieres seguir recibiendo actualizaciones en tiempo real:
            launch {
                productoRepository.getProductos().collect { productos ->
                    Log.d("CatalogoViewModel", "Actualización en tiempo real: ${productos.size}")
                    _todosLosProductos.value = productos
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


    /**
     * ✅ MÉTODO INTELIGENTE: Valida antes de agregar
     * El adapter solo llama a este método, no decide nada
     */
    fun agregarProductoAlCarrito(producto: ProductoResponse) {
        // 1️⃣ Validar stock
        if (producto.stock <= 0) {
            _mensajeUsuario.value = "❌ ${producto.nombre} está sin stock"
            return
        }

        // 2️⃣ Validar si está activo
        if (!producto.activo) {
            _mensajeUsuario.value = "❌ ${producto.nombre} no está disponible"
            return
        }

        // 3️⃣ Validar cantidad en carrito vs stock
        val itemEnCarrito = ProductoRepository.carrito.items
            .find { it.producto.idProducto == producto.idProducto }

        if (itemEnCarrito != null && itemEnCarrito.cantidad >= producto.stock) {
            _mensajeUsuario.value = "⚠️ Ya tienes todo el stock de ${producto.nombre} en el carrito"
            return
        }

        // 4️⃣ TODO OK: Agregar al carrito
        ProductoRepository.carrito.agregarProducto(producto)
        actualizarContadorCarrito()

        // 5️⃣ Mensaje de éxito
        _mensajeUsuario.value = "✅ ${producto.nombre} añadido (${ProductoRepository.carrito.getTotalItems()} items)"
    }

    fun actualizarContadorCarrito() {
        _itemsEnCarrito.value = ProductoRepository.carrito.getTotalItems()
    }

    /**
     * ✅ Limpia el mensaje para que no se muestre dos veces
     */
    fun mensajeMostrado() {
        _mensajeUsuario.value = null
    }

}