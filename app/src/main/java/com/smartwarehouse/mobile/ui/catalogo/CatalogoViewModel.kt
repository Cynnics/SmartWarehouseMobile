package com.smartwarehouse.mobile.ui.catalogo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.model.response.ProductoResponse
import com.smartwarehouse.mobile.data.repository.ProductoRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.launch

class CatalogoViewModel(application: Application) : AndroidViewModel(application) {

    private val productoRepository = ProductoRepository(application)

    // 🔥 Flow de Room convertido a LiveData
    val productosFiltrados = productoRepository.getProductos()
        .asLiveData(viewModelScope.coroutineContext)

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

    init {
        cargarProductos()
        actualizarContadorCarrito()
    }

    fun cargarProductos() {
        viewModelScope.launch {
            _isLoading.value = true

            // Verificar si necesita sincronización
            if (productoRepository.needsSync()) {
                // Primera carga: sincronizar desde API
                val result = productoRepository.syncProductos()
                _syncResult.value = result
            } else {
                // Ya hay datos en cache, opcionalmente sincronizar en segundo plano
                launch {
                    productoRepository.syncProductos()
                }
            }

            _isLoading.value = false
        }
    }

    fun buscarProductos(query: String) {
        // TODO: Implementar búsqueda en Room
        // productoDao.searchProductos(query).asLiveData()
    }

    fun filtrarPorCategoria(categoria: String) {
        // TODO: Implementar filtro en Room
        // productoDao.getProductosByCategoria(categoria).asLiveData()
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