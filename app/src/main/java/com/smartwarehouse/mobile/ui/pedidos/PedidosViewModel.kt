package com.smartwarehouse.mobile.ui.pedidos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.model.response.Pedido
import com.smartwarehouse.mobile.data.repository.AuthRepository
import com.smartwarehouse.mobile.data.repository.PedidoRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.launch

class PedidosViewModel(application: Application) : AndroidViewModel(application) {

    private val pedidoRepository = PedidoRepository(application)
    private val authRepository = AuthRepository(application)

    private val _pedidos = MutableLiveData<NetworkResult<List<Pedido>>>()
    val pedidos: LiveData<NetworkResult<List<Pedido>>> = _pedidos

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _cambioEstadoResult = MutableLiveData<NetworkResult<Boolean>>()
    val cambioEstadoResult: LiveData<NetworkResult<Boolean>> = _cambioEstadoResult

    init {
        cargarPedidos()
    }

    fun cargarPedidos() {
        _isLoading.value = true
        _pedidos.value = NetworkResult.Loading()

        viewModelScope.launch {
            val result = if (authRepository.getUserRole() == "repartidor") {
                // Si es repartidor, solo sus pedidos
                pedidoRepository.getPedidosRepartidor()
            } else {
                // Si es cliente o admin, todos los pedidos
                pedidoRepository.getPedidos()
            }

            _pedidos.value = result
            _isLoading.value = false
        }
    }

    fun filtrarPorEstado(estado: String) {
        if (estado.isEmpty() || estado == "todos") {
            cargarPedidos()
            return
        }

        _isLoading.value = true
        _pedidos.value = NetworkResult.Loading()

        viewModelScope.launch {
            val result = pedidoRepository.getPedidosByEstado(estado)
            _pedidos.value = result
            _isLoading.value = false
        }
    }

    fun cambiarEstadoPedido(idPedido: Int, nuevoEstado: String) {
        _cambioEstadoResult.value = NetworkResult.Loading()

        viewModelScope.launch {
            val result = pedidoRepository.cambiarEstadoPedido(idPedido, nuevoEstado)
            _cambioEstadoResult.value = result

            // Si fue exitoso, recargar la lista
            if (result is NetworkResult.Success) {
                cargarPedidos()
            }
        }
    }

    fun esRepartidor(): Boolean {
        return authRepository.getUserRole() == "repartidor"
    }
}