package com.smartwarehouse.mobile.domain.model

data class Usuario(
    val id: Int,
    val nombre: String,
    val correo: String,
    val rol: String,
    val telefono: String? = null
)