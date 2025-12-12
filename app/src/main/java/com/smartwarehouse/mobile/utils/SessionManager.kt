package com.smartwarehouse.mobile.utils

import android.content.Context
import android.content.SharedPreferences
import com.smartwarehouse.mobile.data.model.response.LoginResponse

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private var memoryToken: String? = null
    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    fun saveAuthToken(loginResponse: LoginResponse) {
        memoryToken = loginResponse.token
        prefs.edit().apply {
            putString(Constants.KEY_TOKEN, loginResponse.token)
            putInt(Constants.KEY_USER_ID, loginResponse.usuario.idUsuario)
            putString(Constants.KEY_USER_EMAIL, loginResponse.usuario.email)
            putString(Constants.KEY_USER_NAME, loginResponse.usuario.nombre)
            putString(Constants.KEY_USER_ROLE, loginResponse.usuario.rol)
            apply()
        }
    }
    fun getAuthToken(): String? {
        return memoryToken
    }
    fun isLoggedIn(): Boolean {
        return memoryToken != null
    }
    fun getUserId(): Int {
        return prefs.getInt(Constants.KEY_USER_ID, -1)
    }
    fun getUserEmail(): String? {
        return prefs.getString(Constants.KEY_USER_EMAIL, null)
    }
    fun getUserName(): String? {
        return prefs.getString(Constants.KEY_USER_NAME, null)
    }

    fun getUserRole(): String? {
        return prefs.getString(Constants.KEY_USER_ROLE, null)
    }
    fun clearSession() {
        memoryToken = null
        prefs.edit().clear().apply()
    }
}