package com.smartwarehouse.mobile.data.repository

import android.content.Context
import com.smartwarehouse.mobile.data.api.ApiClient
import com.smartwarehouse.mobile.data.api.ProductoService
import com.smartwarehouse.mobile.data.local.AppDatabase
import com.smartwarehouse.mobile.data.local.mappers.toEntity
import com.smartwarehouse.mobile.data.local.mappers.toResponse
import com.smartwarehouse.mobile.data.model.response.ProductoResponse
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProductoRepositoryWithRoom(private val context: Context) {

    private val productoService: ProductoService =
        ApiClient.createService(context, ProductoService::class.java)

    private val database = AppDatabase.getInstance(context)
    private val productoDao = database.productoDao()

    /**
     * Obtiene productos con estrategia Cache-First
     * 1. Devuelve datos de cache inmediatamente
     * 2. Actualiza desde API en segundo plano
     */
    fun getProductos(): Flow<List<ProductoResponse>> {
        // Devolver datos de cache
        return productoDao.getAllProductos().map { entities ->
            entities.map { it.toResponse() }
        }
    }

    /**
     * Sincroniza productos desde la API
     */
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

    /**
     * Verifica si necesita sincronización (cache vacío o antiguo)
     */
    suspend fun needsSync(): Boolean {
        return withContext(Dispatchers.IO) {
            productoDao.getProductosCount() == 0
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