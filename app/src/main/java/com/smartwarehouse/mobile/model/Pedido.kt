package com.smartwarehouse.mobile.model

data class Pedido(
    val id: Int,
    val cliente: String,
    val fecha: String,
    val estado: String
)
