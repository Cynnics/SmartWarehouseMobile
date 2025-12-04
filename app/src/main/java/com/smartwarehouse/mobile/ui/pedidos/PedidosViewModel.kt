package com.smartwarehouse.mobile.ui.pedidos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.local.AppDatabase
import com.smartwarehouse.mobile.data.local.mappers.toDomain
import com.smartwarehouse.mobile.data.local.mappers.toEntity
import com.smartwarehouse.mobile.data.model.response.Pedido
import com.smartwarehouse.mobile.data.repository.AuthRepository
import com.smartwarehouse.mobile.data.repository.PedidoRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PedidosViewModel(application: Application) : AndroidViewModel(application) {

    private val pedidoRepository = PedidoRepository(application)
    private val authRepository = AuthRepository(application)
    private val database = AppDatabase.getInstance(application)
    private val pedidoDao = database.pedidoDao()
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // 🔥 Flow de Room (pedidos del repartidor)
    private val _estadoFiltro = MutableLiveData<String>("todos")

    val pedidos: LiveData<List<Pedido>> = _estadoFiltro.switchMap { estado ->
        if (estado == "todos") {
            pedidoDao.getPedidosByRepartidor(authRepository.getUserId())
                .map { it.map { it.toDomain() } }
                .asLiveData(viewModelScope.coroutineContext)
        } else {
            pedidoDao.getPedidosByEstado(estado)
                .map { it.map { it.toDomain() } }
                .asLiveData(viewModelScope.coroutineContext)
        }
    }

    init {
        // Sincronizar al iniciar
        sincronizarPedidos()
    }

    fun sincronizarPedidos() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Sincronizar desde API
                val result = pedidoRepository.getPedidosRepartidor()

                if (result is NetworkResult.Success) {
                    // Guardar en Room
                    val entities = result.data?.map { it.toEntity() } ?: emptyList()
                    entities.forEach { pedidoDao.insertPedido(it) }

                    android.util.Log.d("PedidosVM", "Sincronizados ${entities.size} pedidos")
                } else if (result is NetworkResult.Error) {
                    android.util.Log.e("PedidosVM", "Error: ${result.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("PedidosVM", "Error al sincronizar", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filtrarPorEstado(estado: String) {
        _estadoFiltro.value = estado
    }


}