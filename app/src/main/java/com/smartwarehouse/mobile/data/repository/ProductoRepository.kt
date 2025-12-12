package com.smartwarehouse.mobile.data.repository

import android.content.Context
import android.util.Log
import com.smartwarehouse.mobile.data.api.network.ApiClient
import com.smartwarehouse.mobile.data.api.PedidoService
import com.smartwarehouse.mobile.data.api.ProductoService
import com.smartwarehouse.mobile.data.local.database.AppDatabase
import com.smartwarehouse.mobile.data.local.mappers.toEntity
import com.smartwarehouse.mobile.data.local.mappers.toResponse
import com.smartwarehouse.mobile.data.model.Carrito
import com.smartwarehouse.mobile.data.model.ItemCarrito
import com.smartwarehouse.mobile.data.model.response.CrearPedidoRequest
import com.smartwarehouse.mobile.data.model.response.ItemPedidoRequest
import com.smartwarehouse.mobile.data.model.response.DetallePedidoResponse
import com.smartwarehouse.mobile.data.model.response.ProductoResponse
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repositorio unificado para productos
 * - Gestiona cache local con Room
 * - Sincroniza con la API
 * - Maneja el carrito en memoria
 * - Crea pedidos completos
 */
class ProductoRepository(private val context: Context) {

    private val productoService: ProductoService = ApiClient.createService(context, ProductoService::class.java)
    private val pedidoService: PedidoService = ApiClient.createService(context, PedidoService::class.java)
    private val sessionManager: SessionManager = SessionManager.getInstance(context)

    private val database = AppDatabase.getInstance(context)
    private val productoDao = database.productoDao()
    companion object {
        val carrito = Carrito()
    }


    fun getProductos(): Flow<List<ProductoResponse>> {
        return productoDao.getAllProductos().map { entities ->
            entities.map { it.toResponse() }
        }
    }

    suspend fun syncProductos(): NetworkResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = productoService.getProductos()

                if (response.isSuccessful) {
                    val productos = response.body()?.filter { it.activo && it.stock > 0 }
                        ?: emptyList()

                    // Guardar en cache
                    val entities = productos.map { it.toEntity() }
                    productoDao.insertProductos(entities)

                    NetworkResult.Success(true)
                } else {
                    NetworkResult.Error("Error al sincronizar: ${response.code()}")
                }
            } catch (e: Exception) {
                NetworkResult.Error(handleException(e))
            }
        }
    }

    suspend fun needsSync(): Boolean {
        return withContext(Dispatchers.IO) {
            productoDao.getProductosCount() == 0
        }
    }

    fun buscarProductos(query: String, productos: List<ProductoResponse>): List<ProductoResponse> {
        if (query.isBlank()) return productos

        val queryLower = query.lowercase()
        return productos.filter { producto ->
            producto.nombre.lowercase().contains(queryLower) ||
                    producto.descripcion?.lowercase()?.contains(queryLower) == true ||
                    producto.categoria?.lowercase()?.contains(queryLower) == true
        }
    }


    fun filtrarPorCategoria(categoria: String?, productos: List<ProductoResponse>): List<ProductoResponse> {
        if (categoria.isNullOrBlank() || categoria == "Todas") return productos
        return productos.filter { it.categoria == categoria }
    }

    fun getCategorias(productos: List<ProductoResponse>): List<String> {
        return productos.mapNotNull { it.categoria }.distinct().sorted()
    }


    suspend fun crearPedido(
        direccion: String,
        ciudad: String,
        codigoPostal: String,
        notas: String?,
        latitud: Double?,
        longitud: Double?
    ): NetworkResult<Boolean> {

        return withContext(Dispatchers.IO) {
            try {
                if (carrito.isEmpty()) {
                    return@withContext NetworkResult.Error("El carrito está vacío")
                }

                val idCliente = sessionManager.getUserId()

                val request = CrearPedidoRequest(
                    idCliente = idCliente,
                    items = carrito.items.map {
                        ItemPedidoRequest(
                            idProducto = it.producto.idProducto,
                            cantidad = it.cantidad,
                            subtotal = it.getSubtotal()
                        )
                    },
                    direccion = direccion,
                    ciudad = ciudad,
                    codigoPostal = codigoPostal,
                    notas = notas,
                    latitud = latitud,
                    longitud = longitud,
                    estado = "Pendiente"
                )

                val pedidoResponse = pedidoService.crearPedido(request)

                if (!pedidoResponse.isSuccessful) {
                    return@withContext NetworkResult.Error("Error al crear el pedido")
                }

                val pedidoCreado = pedidoResponse.body()
                    ?: return@withContext NetworkResult.Error("Respuesta del servidor vacía")

                for (item in carrito.items) {

                    val detalle = DetallePedidoResponse(
                        idDetalle = 0,
                        idPedido = pedidoCreado.idPedido,
                        idProducto = item.producto.idProducto,
                        cantidad = item.cantidad,
                        subtotal = item.getSubtotal()
                    )

                    val detalleResp = pedidoService.crearDetallePedido(detalle)

                    if (!detalleResp.isSuccessful) {
                        android.util.Log.e("ProductoRepository", "Error al crear detalle del pedido")
                    }
                }

                carrito.vaciar()

                NetworkResult.Success(true)

            } catch (e: Exception) {
                NetworkResult.Error(handleException(e))
            }
        }
    }

    suspend fun verificarStockDisponible(items: List<ItemCarrito>): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("VerificarStock", "🔍 Verificando stock para ${items.size} productos...")

                for (item in items) {
                    Log.d("VerificarStock", "Verificando: ${item.producto.nombre} - Cantidad solicitada: ${item.cantidad}")

                    val response = productoService.getProductoById(item.producto.idProducto)
                    if (response.isSuccessful) {
                        val productoActual = response.body()
                        if (productoActual == null) {
                            Log.e("VerificarStock", "❌ Body null para producto ${item.producto.idProducto}")
                            return@withContext Pair(false, "No se pudo verificar el producto: ${item.producto.nombre}")
                        }

                        Log.d("VerificarStock", "Stock actual: ${productoActual.stock}")

                        if (productoActual.stock < item.cantidad) {
                            Log.e("VerificarStock", "❌ Stock insuficiente")
                            return@withContext Pair(
                                false,
                                "Stock insuficiente para ${item.producto.nombre}. Disponible: ${productoActual.stock}, Solicitado: ${item.cantidad}"
                            )
                        }

                        Log.d("VerificarStock", "✅ Stock suficiente para ${item.producto.nombre}")
                    } else {
                        Log.e("VerificarStock", "❌ Error en request: ${response.code()}")
                        return@withContext Pair(false, "Error al verificar stock: ${response.code()}")
                    }
                }

                Log.d("VerificarStock", "✅ Stock verificado correctamente para todos los productos")
                Pair(true, "Stock verificado correctamente")
            } catch (e: Exception) {
                Log.e("VerificarStock", "❌ Excepción: ${e.message}", e)
                Pair(false, "Error verificando stock: ${e.message}")
            }
        }
    }

    suspend fun actualizarStockProductos(items: List<ItemCarrito>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ActualizarStock", "🔄 Iniciando actualización de stock para ${items.size} productos...")

                items.forEach { item ->
                    try {
                        Log.d("ActualizarStock", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.d("ActualizarStock", "Producto: ${item.producto.nombre}")
                        Log.d("ActualizarStock", "ID: ${item.producto.idProducto}")
                        Log.d("ActualizarStock", "Cantidad a restar: ${item.cantidad}")

                        Log.d("ActualizarStock", "📥 GET Productos/${item.producto.idProducto}")
                        val response = productoService.getProductoById(item.producto.idProducto)

                        if (response.isSuccessful) {
                            val productoActual = response.body()
                            if (productoActual != null) {
                                Log.d("ActualizarStock", "Stock actual: ${productoActual.stock}")

                                val nuevoStock = productoActual.stock - item.cantidad
                                Log.d("ActualizarStock", "Nuevo stock calculado: $nuevoStock")

                                Log.d("ActualizarStock", "📤 PATCH Productos/${item.producto.idProducto}/stock con valor: $nuevoStock")
                                val updateResponse = productoService.actualizarStock(
                                    item.producto.idProducto,
                                    nuevoStock
                                )

                                if (updateResponse.isSuccessful) {
                                    Log.d("ActualizarStock", "✅ Stock actualizado exitosamente")
                                } else {
                                    Log.e("ActualizarStock", "❌ Error en PATCH: ${updateResponse.code()} - ${updateResponse.message()}")
                                    return@withContext false
                                }
                            } else {
                                Log.e("ActualizarStock", "❌ Body null en GET")
                                return@withContext false
                            }
                        } else {
                            Log.e("ActualizarStock", "❌ Error en GET: ${response.code()}")
                            return@withContext false
                        }
                    } catch (e: Exception) {
                        Log.e("ActualizarStock", "❌ Excepción procesando ${item.producto.nombre}: ${e.message}", e)
                        return@withContext false
                    }
                }

                Log.d("ActualizarStock", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d("ActualizarStock", "✅ TODOS LOS STOCKS ACTUALIZADOS CORRECTAMENTE")
                true
            } catch (e: Exception) {
                Log.e("ActualizarStock", "❌ Error general: ${e.message}", e)
                false
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