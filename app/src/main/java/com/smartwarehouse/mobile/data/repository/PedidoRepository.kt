package com.smartwarehouse.mobile.data.repository

import android.content.Context
import android.util.Log
import com.smartwarehouse.mobile.data.api.network.ApiClient
import com.smartwarehouse.mobile.data.api.PedidoService
import com.smartwarehouse.mobile.data.local.database.AppDatabase
import com.smartwarehouse.mobile.data.model.response.*
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
                        is UnknownHostException -> "Sin conexión a internet"
                        is SocketTimeoutException -> "Tiempo de espera agotado"
                        else -> "Error: ${e.localizedMessage}"
                    }
                )
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

    // PedidoRepository.kt

    suspend fun cambiarEstadoPedido(idPedido: Int, nuevoEstado: String): NetworkResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = pedidoService.cambiarEstado(idPedido, nuevoEstado)

                if (response.isSuccessful) {
                    // 🔥 ACTUALIZAR TAMBIÉN EN ROOM (caché local)
                    val database = AppDatabase.getInstance(context)
                    val pedidoDao = database.pedidoDao()

                    pedidoDao.getPedidoById(idPedido)?.let { pedidoEntity ->
                        val pedidoActualizado = pedidoEntity.copy(estado = nuevoEstado)
                        pedidoDao.updatePedido(pedidoActualizado)
                    }

                    Log.d("PedidoRepo", "✅ Pedido #$idPedido actualizado a $nuevoEstado")
                    NetworkResult.Success(true)
                } else {
                    Log.e("PedidoRepo", "❌ Error HTTP ${response.code()} al cambiar estado")
                    NetworkResult.Error("Error al cambiar estado: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("PedidoRepo", "❌ Excepción al cambiar estado", e)
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
                        is UnknownHostException -> "Sin conexión a internet"
                        is SocketTimeoutException -> "Tiempo de espera agotado"
                        else -> "Error: ${e.localizedMessage}"
                    }
                )
            }
        }
    }

    suspend fun getUsuarioById(idUsuario: Int): NetworkResult<UsuarioResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = pedidoService.getUsuarioById(idUsuario) // Debes agregar este endpoint en tu service
                if (response.isSuccessful) {
                    response.body()?.let {
                        NetworkResult.Success(it)
                    } ?: NetworkResult.Error("Usuario no encontrado")
                } else {
                    NetworkResult.Error("Error al obtener usuario: ${response.code()}")
                }
            } catch (e: Exception) {
                NetworkResult.Error(e.localizedMessage ?: "Error desconocido")
            }
        }
    }

    suspend fun getProductoById(idProducto: Int): NetworkResult<ProductoResponse> {
        return try {
            val response = pedidoService.getProductoById(idProducto) // Llamada a tu API o base de datos
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Error al obtener el producto")
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Error desconocido")
        }
    }





}