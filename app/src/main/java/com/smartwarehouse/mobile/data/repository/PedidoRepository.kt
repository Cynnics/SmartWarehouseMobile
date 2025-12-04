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

                    // 🔥 LOG DETALLADO
                    android.util.Log.d("PedidoRepo", "Pedidos obtenidos: ${pedidos.size}")
                    pedidos.forEach { pedido ->
                        android.util.Log.d("PedidoRepo", """
                            Pedido #${pedido.id}
                            - Dirección: ${pedido.direccionEntrega}
                            - Ciudad: ${pedido.ciudad}
                            - CP: ${pedido.codigoPostal}
                            - Coords: ${pedido.latitud}, ${pedido.longitud}
                        """.trimIndent())
                    }

                    NetworkResult.Success(pedidos)
                } else {
                    android.util.Log.e("PedidoRepo", "Error HTTP: ${response.code()}")
                    NetworkResult.Error("Error al obtener pedidos: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("PedidoRepo", "Error de red", e)
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

    // Obtener pedidos del cliente actual
    suspend fun getPedidosCliente(): NetworkResult<List<Pedido>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = pedidoService.getPedidos()

                if (response.isSuccessful) {
                    val idCliente = sessionManager.getUserId()
                    val pedidos = response.body()
                        ?.filter { it.idCliente == idCliente }
                        ?.map { it.toDomain() }
                        ?: emptyList()

                    android.util.Log.d("PedidoRepo", "Pedidos del cliente $idCliente: ${pedidos.size}")

                    NetworkResult.Success(pedidos)
                } else {
                    NetworkResult.Error("Error al obtener pedidos del cliente")
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

    // Obtener pedido por ID
    suspend fun getPedidoById(idPedido: Int): NetworkResult<Pedido> {
        return withContext(Dispatchers.IO) {
            try {
                val response = pedidoService.getPedidoById(idPedido)

                if (response.isSuccessful) {
                    response.body()?.let { pedidoResponse ->
                        NetworkResult.Success(pedidoResponse.toDomain())
                    } ?: NetworkResult.Error("Pedido no encontrado")
                } else {
                    NetworkResult.Error("Error al obtener el pedido: ${response.code()}")
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





}