package com.smartwarehouse.mobile.data.repository

import android.content.Context
import com.smartwarehouse.mobile.data.api.UsuarioService
import com.smartwarehouse.mobile.data.api.network.ApiClient
import com.smartwarehouse.mobile.data.model.request.ActualizarUsuarioRequest
import com.smartwarehouse.mobile.data.model.response.UsuarioResponse
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.SessionManager
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

    /**
     * Actualiza los datos del usuario
     */
    suspend fun actualizarUsuario(
        idUsuario: Int,
        nombre: String,
        telefono: String
    ): NetworkResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // 1️⃣ Primero obtener el usuario actual
                val sessionManager = SessionManager.getInstance(context)
                val email = sessionManager.getUserEmail() ?: ""
                val rol = sessionManager.getUserRole() ?: ""

                // 2️⃣ Crear objeto completo
                val usuario = UsuarioResponse(
                    idUsuario = idUsuario,
                    nombre = nombre,
                    email = email,
                    rol = rol,
                    telefono = telefono,
                    direccionFacturacion = null

                )

                // 3️⃣ Enviar actualización
                val response = usuarioService.actualizarUsuario(idUsuario, usuario)

                if (response.isSuccessful) {
                    NetworkResult.Success(true)
                } else {
                    NetworkResult.Error("Error al actualizar: ${response.code()}")
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