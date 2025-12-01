package com.smartwarehouse.mobile.ui.pedidos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.model.response.DetallePedidoResponse
import com.smartwarehouse.mobile.data.model.response.Pedido
import com.smartwarehouse.mobile.data.model.response.TotalesPedidoResponse
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

    private val _totales = MutableLiveData<NetworkResult<TotalesPedidoResponse>>()
    val totales: LiveData<NetworkResult<TotalesPedidoResponse>> = _totales

    private val _cambioEstadoResult = MutableLiveData<NetworkResult<Boolean>>()
    val cambioEstadoResult: LiveData<NetworkResult<Boolean>> = _cambioEstadoResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun cargarPedido(idPedido: Int) {
        _isLoading.value = true

        viewModelScope.launch {
            // TODO: Necesitarías añadir este método al repository
            // Por ahora, usamos getPedidos y filtramos
            val result = pedidoRepository.getPedidos()
            _pedido.value = when (result) {
                is NetworkResult.Success -> {
                    val pedido = result.data?.find { it.id == idPedido }
                    if (pedido != null) {
                        NetworkResult.Success(pedido)
                    } else {
                        NetworkResult.Error("Pedido no encontrado")
                    }
                }

                is NetworkResult.Error -> result
                is NetworkResult.Loading -> result
            } as NetworkResult<Pedido>?
            _isLoading.value = false
        }
    }

    fun cargarDetalles(idPedido: Int) {
        viewModelScope.launch {
            val result = pedidoRepository.getDetallesPedido(idPedido)
            _detalles.value = result
        }
    }

    fun cargarTotales(idPedido: Int) {
        viewModelScope.launch {
            val result = pedidoRepository.getTotalesPedido(idPedido)
            _totales.value = result
        }
    }

    fun cambiarEstado(idPedido: Int, nuevoEstado: String) {
        _isLoading.value = true

        viewModelScope.launch {
            val result = pedidoRepository.cambiarEstadoPedido(idPedido, nuevoEstado)
            _cambioEstadoResult.value = result
            _isLoading.value = false
        }
    }

    fun esRepartidor(): Boolean {
        return authRepository.getUserRole() == "repartidor"
    }
}