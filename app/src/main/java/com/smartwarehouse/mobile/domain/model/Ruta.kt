package com.smartwarehouse.mobile.domain.model

data class Ruta(
    val id: Int,
    val repartidor: String,
    val origen: String,
    val destino: String,
    val estado: String
)