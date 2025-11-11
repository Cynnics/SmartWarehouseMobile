package com.smartwarehouse.mobile.model

data class Usuario(
    val id: Int,
    val nombre: String,
    val correo: String,
    val rol: String,
    val telefono: String? = null
)
