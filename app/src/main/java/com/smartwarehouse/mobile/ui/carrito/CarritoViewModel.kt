package com.smartwarehouse.mobile.ui.carrito

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.model.ItemCarrito
import com.smartwarehouse.mobile.data.repository.ProductoRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.launch

class CarritoViewModel(application: Application) : AndroidViewModel(application) {

    private val productoRepository = ProductoRepository(application)
    private val carrito = ProductoRepository.carrito

    private val _items = MutableLiveData<List<ItemCarrito>>()
    val items: LiveData<List<ItemCarrito>> = _items

    private val _subtotal = MutableLiveData<Double>()
    val subtotal: LiveData<Double> = _subtotal

    private val _iva = MutableLiveData<Double>()
    val iva: LiveData<Double> = _iva

    private val _total = MutableLiveData<Double>()
    val total: LiveData<Double> = _total

    private val _crearPedidoResult = MutableLiveData<NetworkResult<Boolean>>()
    val crearPedidoResult: LiveData<NetworkResult<Boolean>> = _crearPedidoResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        actualizarCarrito()
    }

    fun actualizarCarrito() {
        _items.value = carrito.items.toList()
        _subtotal.value = carrito.getSubtotal()
        _iva.value = carrito.getIVA()
        _total.value = carrito.getTotal()
    }

    fun incrementarCantidad(idProducto: Int) {
        carrito.items.find { it.producto.idProducto == idProducto }?.incrementar()
        actualizarCarrito()
    }

    fun decrementarCantidad(idProducto: Int) {
        carrito.items.find { it.producto.idProducto == idProducto }?.decrementar()
        actualizarCarrito()
    }

    fun eliminarItem(idProducto: Int) {
        carrito.eliminarProducto(idProducto)
        actualizarCarrito()
    }

    fun vaciarCarrito() {
        carrito.vaciar()
        actualizarCarrito()
    }

    fun crearPedido(direccionEntrega: String, notas: String?) {
        if (direccionEntrega.isBlank()) {
            _crearPedidoResult.value = NetworkResult.Error("La dirección de entrega es obligatoria")
            return
        }

        if (carrito.isEmpty()) {
            _crearPedidoResult.value = NetworkResult.Error("El carrito está vacío")
            return
        }

        _isLoading.value = true
        _crearPedidoResult.value = NetworkResult.Loading()

        viewModelScope.launch {
            val result = productoRepository.crearPedido(direccionEntrega, notas)
            _crearPedidoResult.value = result
            _isLoading.value = false

            if (result is NetworkResult.Success) {
                actualizarCarrito() // El carrito ya está vacío
            }
        }
    }

    fun carritoEstaVacio(): Boolean = carrito.isEmpty()
}