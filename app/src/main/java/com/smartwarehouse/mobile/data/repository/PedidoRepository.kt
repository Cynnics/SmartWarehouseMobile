package com.smartwarehouse.mobile.data.repository

import android.content.Context
import com.google.gson.Gson
import com.smartwarehouse.mobile.data.api.ApiClient
import com.smartwarehouse.mobile.data.api.PedidoService
import com.smartwarehouse.mobile.data.model.response.*
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PedidoRepository(private val context: Context) {

    private val pedidoService: PedidoService = ApiClient.createService(context, PedidoService::class.java)
    private val sessionManager = SessionManager.getInstance(context)

    // Obtener todos los pedidos
    suspend fun getPedidos(): NetworkResult<List<Pedido>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = pedidoService.getPedidos()

                if (response.isSuccessful) {
                    val pedidos = response.body()?.map { it.toDomain() } ?: emptyList()
                    NetworkResult.Success(pedidos)
                } else {
                    NetworkResult.Error("Error al obtener pedidos: ${response.code()}")
                }
            } catch (e: Exception) {
                NetworkResult.Error(
                    when (e) {
                        is java.net.UnknownHostException -> "Sin conexión a internet"
                        is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                        else -> "Error: ${e.localizedMessage}"
                    }
                )
            }
        }
    }

    // Obtener pedidos filtrados por estado
    suspend fun getPedidosByEstado(estado: String): NetworkResult<List<Pedido>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = pedidoService.getPedidosByEstado(estado)

                if (response.isSuccessful) {
                    val pedidos = response.body()?.map { it.toDomain() } ?: emptyList()
                    NetworkResult.Success(pedidos)
                } else {
                    NetworkResult.Error("Error al filtrar pedidos")
                }
            } catch (e: Exception) {
                NetworkResult.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    // Obtener pedidos del repartidor actual
    suspend fun getPedidosRepartidor(): NetworkResult<List<Pedido>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = pedidoService.getPedidos()

                if (response.isSuccessful) {
                    val idRepartidor = sessionManager.getUserId()
                    val pedidos = response.body()
                        ?.filter { it.idRepartidor == idRepartidor }
                        ?.map { it.toDomain() }
                        ?: emptyList()
                    NetworkResult.Success(pedidos)
                } else {
                    NetworkResult.Error("Error al obtener pedidos del repartidor")
                }
            } catch (e: Exception) {
                NetworkResult.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    // Obtener detalles de un pedido
    suspend fun getDetallesPedido(idPedido: Int): NetworkResult<List<DetallePedidoResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = pedidoService.getDetallesPedido(idPedido)

                if (response.isSuccessful) {
                    NetworkResult.Success(response.body() ?: emptyList())
                } else {
                    NetworkResult.Error("Error al obtener detalles")
                }
            } catch (e: Exception) {
                NetworkResult.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    // Obtener totales del pedido
    suspend fun getTotalesPedido(idPedido: Int): NetworkResult<TotalesPedidoResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = pedidoService.getTotalesPedido(idPedido)

                if (response.isSuccessful) {
                    response.body()?.let {
                        NetworkResult.Success(it)
                    } ?: NetworkResult.Error("No se encontraron totales")
                } else {
                    NetworkResult.Error("Error al obtener totales")
                }
            } catch (e: Exception) {
                NetworkResult.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    // Cambiar estado del pedido (para repartidores)
    suspend fun cambiarEstadoPedido(idPedido: Int, nuevoEstado: String): NetworkResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = pedidoService.cambiarEstado(idPedido, nuevoEstado)

                if (response.isSuccessful) {
                    NetworkResult.Success(true)
                } else {
                    NetworkResult.Error("Error al cambiar estado")
                }
            } catch (e: Exception) {
                NetworkResult.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    fun getPedidoById(idPedido: Int): NetworkResult<Pedido> {
        // ejemplo de retorno de prueba
        return NetworkResult.Success(Pedido(estado = EstadoPedido.ENTREGADO, fechaEntrega = "", fechaPedido = "", id = 0, idCliente = 0, idRepartidor = 0)) // o NetworkResult.Error("mensaje de error")
    }





}