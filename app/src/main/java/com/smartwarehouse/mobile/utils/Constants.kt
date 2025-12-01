package com.smartwarehouse.mobile.utils

object Constants {

    // API Configuration
    // 🔥 CAMBIA ESTA IP SEGÚN EL EQUIPO DONDE EJECUTES LA API
    // Opción 1: Para emulador Android Studio
    //const val BASE_URL = "http://10.0.2.2:5294/"

    // Opción 2: Para dispositivo físico (cambia la IP según tu red)
    const val BASE_URL = "http://192.168.1.135:5294/api/"

    // Opción 3: Para usar con túnel ngrok (desarrollo avanzado)
    // const val BASE_URL = "https://tu-url.ngrok.io/api/"

    // 💡 TIP: Para obtener tu IP local:
    // Windows: ipconfig (busca IPv4)
    // Mac/Linux: ifconfig o hostname -I

    // JWT Configuration
    const val TOKEN_EXPIRY_HOURS = 8

    // Preferences
    const val PREFS_NAME = "smartwarehouse_prefs"
    const val KEY_TOKEN = "auth_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_ROLE = "user_role"
    const val KEY_TOKEN_EXPIRY = "token_expiry"

    // User Roles
    const val ROLE_ADMIN = "admin"
    const val ROLE_EMPLEADO = "empleado"
    const val ROLE_REPARTIDOR = "repartidor"
    const val ROLE_CLIENTE = "cliente"

    // Request Timeouts
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}