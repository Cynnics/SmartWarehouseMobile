package com.smartwarehouse.mobile.domain.model

data class Pedido(
    val id: Int,
    val cliente: String,
    val fecha: String,
    val estado: String
)