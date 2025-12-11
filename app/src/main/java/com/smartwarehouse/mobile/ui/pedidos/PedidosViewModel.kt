package com.smartwarehouse.mobile.ui.pedidos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.local.database.AppDatabase
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
    private val _estadoFiltro = MutableLiveData<String>("todos")

    val pedidos: LiveData<List<Pedido>> = _estadoFiltro.switchMap { estado ->
        val userRole = authRepository.getUserRole()
        val userId = authRepository.getUserId()

        when {
            userRole == "cliente" -> {
                if (estado == "todos") {
                    pedidoDao.getPedidosByCliente(userId)
                        .map { it.map { entity -> entity.toDomain() } }
                        .asLiveData(viewModelScope.coroutineContext)
                } else {
                    pedidoDao.getPedidosByEstado(estado)
                        .map { it.filter { p -> p.idCliente == userId }.map { entity -> entity.toDomain() } }
                        .asLiveData(viewModelScope.coroutineContext)
                }
            }
            userRole == "repartidor" -> {
                if (estado == "todos") {
                    pedidoDao.getPedidosByRepartidor(userId)
                        .map { it.map { entity -> entity.toDomain() } }
                        .asLiveData(viewModelScope.coroutineContext)
                } else {
                    pedidoDao.getPedidosByEstado(estado)
                        .map { it.filter { p -> p.idRepartidor == userId }.map { entity -> entity.toDomain() } }
                        .asLiveData(viewModelScope.coroutineContext)
                }
            }
            else -> {
                if (estado == "todos") {
                    pedidoDao.getAllPedidos()
                        .map { it.map { entity -> entity.toDomain() } }
                        .asLiveData(viewModelScope.coroutineContext)
                } else {
                    pedidoDao.getPedidosByEstado(estado)
                        .map { it.map { entity -> entity.toDomain() } }
                        .asLiveData(viewModelScope.coroutineContext)
                }
            }
        }
    }

    init {
        sincronizarPedidos()
    }

    // ✅ SIMPLIFICADO - Sin isLoading redundante
    fun sincronizarPedidos(forzarTodos: Boolean = false) {
        viewModelScope.launch {
            val userRole = authRepository.getUserRole()
            val result = when {
                forzarTodos -> pedidoRepository.getPedidos()
                userRole == "cliente" -> pedidoRepository.getPedidosCliente()
                userRole == "repartidor" -> pedidoRepository.getPedidosRepartidor()
                else -> pedidoRepository.getPedidos()
            }

            if (result is NetworkResult.Success) {
                val entities = result.data?.map { it.toEntity() } ?: emptyList()
                entities.forEach { pedidoDao.insertPedido(it) }
            }
        }
    }

    fun filtrarPorEstado(estado: String) {
        _estadoFiltro.value = estado
    }
}