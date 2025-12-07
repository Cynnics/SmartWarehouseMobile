package com.smartwarehouse.mobile.data.repository

import android.content.Context
import com.smartwarehouse.mobile.data.api.network.ApiClient
import com.smartwarehouse.mobile.data.api.UsuarioService
import com.smartwarehouse.mobile.data.model.response.UsuarioResponse
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsuarioRepository(private val context: Context) {

    private val usuarioService: UsuarioService =
        ApiClient.createService(context, UsuarioService::class.java)

    /**
     * Obtiene la lista de repartidores
     */
    suspend fun getRepartidores(): NetworkResult<List<UsuarioResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = usuarioService.getRepartidores()

                if (response.isSuccessful) {
                    NetworkResult.Success(response.body() ?: emptyList())
                } else {
                    NetworkResult.Error("Error al obtener repartidores: ${response.code()}")
                }
            } catch (e: Exception) {
                NetworkResult.Error(handleException(e))
            }
        }
    }

    /**
     * Obtiene todos los usuarios (solo admin)
     */
    suspend fun getUsuarios(): NetworkResult<List<UsuarioResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = usuarioService.getUsuarios()

                if (response.isSuccessful) {
                    NetworkResult.Success(response.body() ?: emptyList())
                } else {
                    NetworkResult.Error("Error al obtener usuarios: ${response.code()}")
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