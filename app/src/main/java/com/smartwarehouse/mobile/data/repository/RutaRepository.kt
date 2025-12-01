package com.smartwarehouse.mobile.data.repository

import android.content.Context
import com.smartwarehouse.mobile.data.api.ApiClient
import com.smartwarehouse.mobile.data.api.RutaService
import com.smartwarehouse.mobile.data.api.UbicacionService
import com.smartwarehouse.mobile.data.model.response.*
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RutaRepository(private val context: Context) {

    private val rutaService: RutaService = ApiClient.createService(context, RutaService::class.java)
    private val ubicacionService: UbicacionService = ApiClient.createService(context, UbicacionService::class.java)
    private val sessionManager = SessionManager.getInstance(context)

    // Obtener todas las rutas
    suspend fun getRutas(): NetworkResult<List<Ruta>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = rutaService.getRutas()

                if (response.isSuccessful) {
                    val rutas = response.body()?.map { it.toDomain() } ?: emptyList()
                    NetworkResult.Success(rutas)
                } else {
                    NetworkResult.Error("Error al obtener rutas: ${response.code()}")
                }
            } catch (e: Exception) {
                NetworkResult.Error(handleException(e))
            }
        }
    }

    // Obtener rutas del repartidor actual
    suspend fun getRutasRepartidor(): NetworkResult<List<Ruta>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = rutaService.getRutas()

                if (response.isSuccessful) {
                    val idRepartidor = sessionManager.getUserId()
                    val rutas = response.body()
                        ?.filter { it.idRepartidor == idRepartidor }
                        ?.map { it.toDomain() }
                        ?: emptyList()
                    NetworkResult.Success(rutas)
                } else {
                    NetworkResult.Error("Error al obtener rutas del repartidor")
                }
            } catch (e: Exception) {
                NetworkResult.Error(handleException(e))
            }
        }
    }

    // Obtener ruta por ID
    suspend fun getRutaById(idRuta: Int): NetworkResult<Ruta> {
        return withContext(Dispatchers.IO) {
            try {
                val response = rutaService.getRutaById(idRuta)

                if (response.isSuccessful) {
                    response.body()?.let {
                        NetworkResult.Success(it.toDomain())
                    } ?: NetworkResult.Error("Ruta no encontrada")
                } else {
                    NetworkResult.Error("Error al obtener ruta")
                }
            } catch (e: Exception) {
                NetworkResult.Error(handleException(e))
            }
        }
    }

    // Obtener pedidos de una ruta
    suspend fun getPedidosDeRuta(idRuta: Int): NetworkResult<List<Pedido>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = rutaService.getPedidosDeRuta(idRuta)

                if (response.isSuccessful) {
                    val pedidos = response.body()?.map { it.toDomain() } ?: emptyList()
                    NetworkResult.Success(pedidos)
                } else {
                    NetworkResult.Error("Error al obtener pedidos de la ruta")
                }
            } catch (e: Exception) {
                NetworkResult.Error(handleException(e))
            }
        }
    }

    // Cambiar estado de la ruta
    suspend fun cambiarEstadoRuta(idRuta: Int, nuevoEstado: String): NetworkResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val response = rutaService.cambiarEstadoRuta(idRuta, nuevoEstado)

                if (response.isSuccessful) {
                    NetworkResult.Success(true)
                } else {
                    NetworkResult.Error("Error al cambiar estado de la ruta")
                }
            } catch (e: Exception) {
                NetworkResult.Error(handleException(e))
            }
        }
    }

    // Enviar ubicación GPS del repartidor
    suspend fun enviarUbicacion(latitud: Double, longitud: Double): NetworkResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val idRepartidor = sessionManager.getUserId()
                val request = CrearUbicacionRequest(
                    idRepartidor = idRepartidor,
                    latitud = latitud,
                    longitud = longitud
                )

                val response = ubicacionService.crearUbicacion(request)

                if (response.isSuccessful) {
                    NetworkResult.Success(true)
                } else {
                    NetworkResult.Error("Error al enviar ubicación")
                }
            } catch (e: Exception) {
                NetworkResult.Error(handleException(e))
            }
        }
    }

    // Obtener última ubicación del repartidor
    suspend fun getUltimaUbicacion(): NetworkResult<UbicacionRepartidorResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val idRepartidor = sessionManager.getUserId()
                val response = ubicacionService.getUltimaUbicacion(idRepartidor)

                if (response.isSuccessful) {
                    response.body()?.let {
                        NetworkResult.Success(it)
                    } ?: NetworkResult.Error("No se encontró ubicación")
                } else {
                    NetworkResult.Error("Error al obtener ubicación")
                }
            } catch (e: Exception) {
                NetworkResult.Error(handleException(e))
            }
        }
    }

    // Obtener ubicaciones del repartidor de una ruta
    suspend fun getUbicacionesDeRuta(idRuta: Int): NetworkResult<List<UbicacionRepartidorResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = rutaService.getUbicacionesDeRuta(idRuta)

                if (response.isSuccessful) {
                    NetworkResult.Success(response.body() ?: emptyList())
                } else {
                    NetworkResult.Error("Error al obtener ubicaciones")
                }
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