package com.smartwarehouse.mobile.data.repository

import android.content.Context
import com.google.gson.Gson
import com.smartwarehouse.mobile.data.api.ApiClient
import com.smartwarehouse.mobile.data.api.AuthService
import com.smartwarehouse.mobile.data.model.request.LoginRequest
import com.smartwarehouse.mobile.data.model.response.ApiErrorResponse
import com.smartwarehouse.mobile.data.model.response.LoginResponse
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {

    private val authService: AuthService = ApiClient.createService(context, AuthService::class.java)
    private val sessionManager: SessionManager = SessionManager.getInstance(context)

    suspend fun login(email: String, password: String): NetworkResult<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val loginRequest = LoginRequest(email, password)
                val response = authService.login(loginRequest)

                if (response.isSuccessful) {
                    response.body()?.let { loginResponse ->
                        // Guardar token y datos de usuario
                        sessionManager.saveAuthToken(loginResponse)
                        NetworkResult.Success(loginResponse)
                    } ?: NetworkResult.Error("Respuesta vacía del servidor")
                } else {
                    // Parsear error de la API
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val errorResponse = Gson().fromJson(errorBody, ApiErrorResponse::class.java)
                        errorResponse.message
                    } catch (e: Exception) {
                        "Error en el inicio de sesión"
                    }
                    NetworkResult.Error(errorMessage)
                }
            } catch (e: Exception) {
                NetworkResult.Error(
                    when (e) {
                        is java.net.UnknownHostException -> "No hay conexión a internet"
                        is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                        else -> "Error: ${e.localizedMessage ?: "Desconocido"}"
                    }
                )
            }
        }
    }

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    fun logout() {
        sessionManager.clearSession()
    }

    fun getUserRole(): String? = sessionManager.getUserRole()

    fun getUserName(): String? = sessionManager.getUserName()
}