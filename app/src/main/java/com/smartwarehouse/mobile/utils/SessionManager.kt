package com.smartwarehouse.mobile.utils

import android.content.Context
import android.content.SharedPreferences
import com.smartwarehouse.mobile.data.model.response.LoginResponse

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Guardar token y datos de usuario
    fun saveAuthToken(loginResponse: LoginResponse) {
        prefs.edit().apply {
            putString(Constants.KEY_TOKEN, loginResponse.token)
            putInt(Constants.KEY_USER_ID, loginResponse.usuario.idUsuario)
            putString(Constants.KEY_USER_EMAIL, loginResponse.usuario.email)
            putString(Constants.KEY_USER_NAME, loginResponse.usuario.nombre)
            putString(Constants.KEY_USER_ROLE, loginResponse.usuario.rol)
            putLong(Constants.KEY_TOKEN_EXPIRY, System.currentTimeMillis() + (Constants.TOKEN_EXPIRY_HOURS * 3600000))
            //putLong(Constants.KEY_TOKEN_EXPIRY, System.currentTimeMillis() + (Constants.TOKEN_EXPIRY_MINUTES_TEST * 60 * 1000))
            apply()
        }
    }

    // Obtener token
    fun getAuthToken(): String? {
        return prefs.getString(Constants.KEY_TOKEN, null)
    }

    // Verificar si el usuario está autenticado
    fun isLoggedIn(): Boolean {
        val token = getAuthToken()
        val expiry = prefs.getLong(Constants.KEY_TOKEN_EXPIRY, 0)
        return token != null && System.currentTimeMillis() < expiry
    }

    // Verificar si el token ha expirado
    fun isTokenExpired(): Boolean {
        val expiry = prefs.getLong(Constants.KEY_TOKEN_EXPIRY, 0)
        return System.currentTimeMillis() >= expiry
    }

    // Obtener ID del usuario
    fun getUserId(): Int {
        return prefs.getInt(Constants.KEY_USER_ID, -1)
    }

    // Obtener email del usuario
    fun getUserEmail(): String? {
        return prefs.getString(Constants.KEY_USER_EMAIL, null)
    }

    // Obtener nombre del usuario
    fun getUserName(): String? {
        return prefs.getString(Constants.KEY_USER_NAME, null)
    }

    // Obtener rol del usuario
    fun getUserRole(): String? {
        return prefs.getString(Constants.KEY_USER_ROLE, null)
    }

    // Verificar si es repartidor
    fun isRepartidor(): Boolean {
        return getUserRole() == Constants.ROLE_REPARTIDOR
    }

    // Verificar si es cliente
    fun isCliente(): Boolean {
        return getUserRole() == Constants.ROLE_CLIENTE
    }

    // Cerrar sesión
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}