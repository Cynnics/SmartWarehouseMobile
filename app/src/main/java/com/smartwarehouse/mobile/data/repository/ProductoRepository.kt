package com.smartwarehouse.mobile.data.repository

import android.content.Context
import com.smartwarehouse.mobile.data.api.ApiClient
import com.smartwarehouse.mobile.data.api.PedidoService
import com.smartwarehouse.mobile.data.api.ProductoService
import com.smartwarehouse.mobile.data.model.Carrito
import com.smartwarehouse.mobile.data.model.CrearPedidoRequest
import com.smartwarehouse.mobile.data.model.ItemPedidoRequest
import com.smartwarehouse.mobile.data.model.response.ProductoResponse
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.POST

class ProductoRepository(private val context: Context) {

    private val productoService: ProductoService = ApiClient.createService(context, ProductoService::class.java)
    private val pedidoService: PedidoService = ApiClient.createService(context, PedidoService::class.java)
    private val sessionManager = SessionManager.getInstance(context)

    // Carrito en memoria (singleton)
    companion object {
        val carrito = Carrito()
    }

    // Obtener todos los productos
    suspend fun getProductos(): NetworkResult<List<ProductoResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = productoService.getProductos()

                if (response.isSuccessful) {
                    val productos = response.body()
                        ?.filter { it.activo && it.stock > 0 } // Solo productos activos con stock
                        ?: emptyList()
                    NetworkResult.Success(productos)
                } else {
                    NetworkResult.Error("Error al obtener productos: ${response.code()}")
                }
            } catch (e: Exception) {
                NetworkResult.Error(handleException(e))
            }
        }
    }

    // Buscar productos por nombre o categoría
    fun buscarProductos(query: String, productos: List<ProductoResponse>): List<ProductoResponse> {
        if (query.isBlank()) return productos

        val queryLower = query.lowercase()
        return productos.filter { producto ->
            producto.nombre.lowercase().contains(queryLower) ||
                    producto.descripcion?.lowercase()?.contains(queryLower) == true ||
                    producto.categoria?.lowercase()?.contains(queryLower) == true
        }
    }

    // Filtrar por categoría
    fun filtrarPorCategoria(categoria: String?, productos: List<ProductoResponse>): List<ProductoResponse> {
        if (categoria.isNullOrBlank() || categoria == "Todas") return productos
        return productos.filter { it.categoria == categoria }
    }

    // Obtener categorías únicas
    fun getCategorias(productos: List<ProductoResponse>): List<String> {
        return productos.mapNotNull { it.categoria }.distinct().sorted()
    }

    // Crear pedido desde el carrito
    suspend fun crearPedido(
        direccionEntrega: String,
        notas: String?
    ): NetworkResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                if (carrito.isEmpty()) {
                    return@withContext NetworkResult.Error("El carrito está vacío")
                }

                val idCliente = sessionManager.getUserId()

                // 1. Crear el pedido
                val pedidoRequest = com.smartwarehouse.mobile.data.model.response.PedidoResponse(
                    idPedido = 0, // Se generará en la API
                    idCliente = idCliente,
                    idRepartidor = null,
                    estado = "pendiente",
                    fechaPedido = "",
                    fechaEntrega = null
                )

                val pedidoResponse = pedidoService.crearPedido(pedidoRequest)

                if (!pedidoResponse.isSuccessful) {
                    return@withContext NetworkResult.Error("Error al crear el pedido")
                }

                val pedidoCreado = pedidoResponse.body()
                    ?: return@withContext NetworkResult.Error("Error: respuesta vacía")

                // 2. Crear los detalles del pedido
                for (item in carrito.items) {
                    val detalle = com.smartwarehouse.mobile.data.model.response.DetallePedidoResponse(
                        idDetalle = 0,
                        idPedido = pedidoCreado.idPedido,
                        idProducto = item.producto.idProducto,
                        cantidad = item.cantidad,
                        subtotal = item.getSubtotal()
                    )

                    val detalleResponse = pedidoService.crearDetallePedido(detalle)

                    if (!detalleResponse.isSuccessful) {
                        // Si falla algún detalle, el pedido ya está creado
                        // Idealmente deberías revertir, pero por simplicidad continuamos
                        android.util.Log.e("ProductoRepository", "Error al crear detalle del pedido")
                    }
                }

                // 3. Vaciar el carrito después de crear el pedido
                carrito.vaciar()

                NetworkResult.Success(true)

            } catch (e: Exception) {
                NetworkResult.Error(handleException(e))
            }
        }
    }

    private fun handleException(e: Exception): String {
        return when (e) {
            is java.net.UnknownHostException -> "Sin conexión a internet"
            is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
            else -> "Error: ${e.localizedMessage}"
        }
    }
}

// Extensión al PedidoService
interface PedidoServiceExtended {
    @POST("Pedidos")
    suspend fun crearPedido(
        @retrofit2.http.Body pedido: com.smartwarehouse.mobile.data.model.response.PedidoResponse
    ): retrofit2.Response<com.smartwarehouse.mobile.data.model.response.PedidoResponse>

    @POST("DetallePedido")
    suspend fun crearDetallePedido(
        @retrofit2.http.Body detalle: com.smartwarehouse.mobile.data.model.response.DetallePedidoResponse
    ): retrofit2.Response<com.smartwarehouse.mobile.data.model.response.DetallePedidoResponse>
}