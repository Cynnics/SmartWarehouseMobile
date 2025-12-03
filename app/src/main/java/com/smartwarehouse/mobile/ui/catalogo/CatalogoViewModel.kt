package com.smartwarehouse.mobile.ui.catalogo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.model.response.ProductoResponse
import com.smartwarehouse.mobile.data.repository.ProductoRepository
import com.smartwarehouse.mobile.data.repository.ProductoRepositoryWithRoom
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.launch

class CatalogoViewModel(application: Application) : AndroidViewModel(application) {

    private val productoRepository = ProductoRepositoryWithRoom(application)

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

    fun agregarAlCarrito(producto: ProductoResponse) {
        ProductoRepository.carrito.agregarProducto(producto)
        actualizarContadorCarrito()
    }

    fun actualizarContadorCarrito() {
        _itemsEnCarrito.value = ProductoRepository.carrito.getTotalItems()

    }
}