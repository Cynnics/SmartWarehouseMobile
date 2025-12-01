package com.smartwarehouse.mobile.data.model.response

import com.google.gson.annotations.SerializedName

data class ProductoResponse(
    @SerializedName("idProducto")
    val idProducto: Int,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("precio")
    val precio: Double,

    @SerializedName("stock")
    val stock: Int,

    @SerializedName("categoria")
    val categoria: String?,

    @SerializedName("activo")
    val activo: Boolean = true
) {
    fun getPrecioFormateado(): String = String.format("%.2f €", precio)

    fun tieneStock(): Boolean = stock > 0

    fun getStockTexto(): String = when {
        stock > 10 -> "En stock"
        stock > 0 -> "Últimas unidades ($stock)"
        else -> "Sin stock"
    }
}