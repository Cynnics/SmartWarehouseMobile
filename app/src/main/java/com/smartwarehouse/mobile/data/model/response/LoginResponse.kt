package com.smartwarehouse.mobile.data.model.response

data class LoginResponse(
    val token: String,
    val usuario: UsuarioResponse
)

data class UsuarioResponse (
    val idUsuario: Int,
    val nombre: String,
    val email: String,
    val rol: String,
    val telefono : String
)

// Modelo para respuestas de error de la API
data class ApiErrorResponse(
    val message: String,
    val errors: Map<String, List<String>>? = null
)