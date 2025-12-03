package com.smartwarehouse.mobile.domain.model

data class PedidoModel(
    val id: Int,
    val cliente: String,
    val fecha: String,
    val estado: String
)