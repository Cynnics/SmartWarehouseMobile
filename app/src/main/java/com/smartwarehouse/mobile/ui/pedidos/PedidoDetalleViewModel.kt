package com.smartwarehouse.mobile.ui.pedidos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.model.response.DetallePedidoResponse
import com.smartwarehouse.mobile.data.model.response.Pedido
import com.smartwarehouse.mobile.data.model.response.TotalesPedidoResponse
import com.smartwarehouse.mobile.data.model.response.UsuarioResponse
import com.smartwarehouse.mobile.data.repository.AuthRepository
import com.smartwarehouse.mobile.data.repository.PedidoRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.launch

class PedidoDetalleViewModel(application: Application) : AndroidViewModel(application) {

    private val pedidoRepository = PedidoRepository(application)
    private val authRepository = AuthRepository(application)
    private val _pedido = MutableLiveData<NetworkResult<Pedido>>()
    val pedido: LiveData<NetworkResult<Pedido>> = _pedido
    private val _detalles = MutableLiveData<NetworkResult<List<DetallePedidoResponse>>>()
    val detalles: LiveData<NetworkResult<List<DetallePedidoResponse>>> = _detalles
    private val _productosMap = MutableLiveData<Map<Int, String>>()
    val productosMap: LiveData<Map<Int, String>> = _productosMap
    private val _cliente = MutableLiveData<NetworkResult<UsuarioResponse>>()
    val cliente: LiveData<NetworkResult<UsuarioResponse>> = _cliente
    private val _totales = MutableLiveData<NetworkResult<TotalesPedidoResponse>>()
    val totales: LiveData<NetworkResult<TotalesPedidoResponse>> = _totales

    private val _cambioEstadoResult = MutableLiveData<NetworkResult<Boolean>>()
    val cambioEstadoResult: LiveData<NetworkResult<Boolean>> = _cambioEstadoResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun cargarPedido(idPedido: Int) {
        _isLoading.value = true

        viewModelScope.launch {
            val result = pedidoRepository.getPedidoById(idPedido)
            _pedido.value = result
            if (result is NetworkResult.Success) {
                result.data?.let { pedido ->
                    cargarCliente(pedido.idCliente)
                }
            }

            _isLoading.value = false
        }
    }

    fun cargarDetalles(idPedido: Int) {
        viewModelScope.launch {
            val result = pedidoRepository.getDetallesPedido(idPedido)
            if (result is NetworkResult.Success) {
                val detalles = result.data ?: emptyList()

                val mapProductos = mutableMapOf<Int, String>()
                for (detalle in detalles) {
                    val productoResult = pedidoRepository.getProductoById(detalle.idProducto)
                    if (productoResult is NetworkResult.Success) {
                        val nombreProducto = productoResult.data?.nombre ?: "Producto #${detalle.idProducto}"
                        mapProductos[detalle.idProducto] = nombreProducto
                    } else {
                        mapProductos[detalle.idProducto] = "Producto #${detalle.idProducto}"
                    }
                }

                _productosMap.value = mapProductos
            }

            _detalles.value = result
        }
    }


    fun cargarTotales(idPedido: Int) {
        viewModelScope.launch {
            val result = pedidoRepository.getTotalesPedido(idPedido)
            _totales.value = result
        }
    }

    fun cargarCliente(idCliente: Int) {
        viewModelScope.launch {
            _cliente.value = NetworkResult.Loading()
            val result = pedidoRepository.getUsuarioById(idCliente)
            _cliente.value = result
        }
    }

    fun cambiarEstado(idPedido: Int, nuevoEstado: String) {
        _isLoading.value = true

        viewModelScope.launch {
            val result = pedidoRepository.cambiarEstadoPedido(idPedido, nuevoEstado)
            _cambioEstadoResult.value = result

            if (result is NetworkResult.Success) {
                cargarPedido(idPedido)
            }

            _isLoading.value = false
        }
    }

    fun avanzarAlSiguienteEstado(idPedido: Int) {
        val pedidoActual = (_pedido.value as? NetworkResult.Success)?.data
        pedidoActual?.let { pedido ->
            val siguienteEstado = pedido.getEstadoSiguiente()
            if (siguienteEstado != null) {
                val estadoString = when (siguienteEstado) {
                    com.smartwarehouse.mobile.data.model.response.EstadoPedido.PENDIENTE -> "pendiente"
                    com.smartwarehouse.mobile.data.model.response.EstadoPedido.PREPARADO -> "preparado"
                    com.smartwarehouse.mobile.data.model.response.EstadoPedido.EN_REPARTO -> "en_reparto"
                    com.smartwarehouse.mobile.data.model.response.EstadoPedido.ENTREGADO -> "entregado"
                }
                cambiarEstado(idPedido, estadoString)
            }
        }
    }

    fun esRepartidor(): Boolean {
        return authRepository.getUserRole() == "repartidor"
    }

    fun esCliente(): Boolean {
        return authRepository.getUserRole() == "cliente"
    }

    fun esAdministradorEmpleado(): Boolean {
        val role = authRepository.getUserRole()
        return role == "admin" || role == "empleado"
    }

    fun getUserId(): Int {
        return authRepository.getUserId()
    }
}