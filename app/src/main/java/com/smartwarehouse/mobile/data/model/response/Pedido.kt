package com.smartwarehouse.mobile.data.model.response

import com.google.gson.annotations.SerializedName

// Modelo principal de Pedido (debe coincidir con tu API)
data class PedidoResponse(
    @SerializedName("idPedido")
    val idPedido: Int,

    @SerializedName("idCliente")
    val idCliente: Int,

    @SerializedName("idRepartidor")
    val idRepartidor: Int?,

    @SerializedName("estado")
    val estado: String, // "pendiente", "preparado", "en_reparto", "entregado"

    @SerializedName("fechaPedido")
    val fechaPedido: String, // Formato: "2024-11-30T10:30:00"

    @SerializedName("fechaEntrega")
    val fechaEntrega: String?
)

// Modelo de detalle de pedido CON INFORMACIÓN DEL PRODUCTO
data class DetallePedidoResponse(
    @SerializedName("idDetalle")
    val idDetalle: Int,

    @SerializedName("idPedido")
    val idPedido: Int,

    @SerializedName("idProducto")
    val idProducto: Int,

    @SerializedName("cantidad")
    val cantidad: Int,

    @SerializedName("subtotal")
    val subtotal: Double
)

// Modelo extendido para la UI (con nombre del producto)
data class DetallePedidoExtendido(
    val idDetalle: Int,
    val idPedido: Int,
    val idProducto: Int,
    val nombreProducto: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)

// Modelo de totales del pedido
data class TotalesPedidoResponse(
    @SerializedName("subtotal")
    val subtotal: Double,

    @SerializedName("iva")
    val iva: Double,

    @SerializedName("total")
    val total: Double
)

// Request para cambiar estado
data class CambiarEstadoPedidoRequest(
    val nuevoEstado: String
)

// Modelo de dominio (para la UI)
data class Pedido(
    val id: Int,
    val idCliente: Int,
    val idRepartidor: Int?,
    val estado: EstadoPedido,
    val fechaPedido: String,
    val fechaEntrega: String?,
    val nombreCliente: String? = null,
    val direccionEntrega: String? = null,
    val telefonoCliente: String? = null
) {
    fun getEstadoColor(): Int {
        return when (estado) {
            EstadoPedido.PENDIENTE -> android.graphics.Color.parseColor("#FFC107")
            EstadoPedido.PREPARADO -> android.graphics.Color.parseColor("#2196F3")
            EstadoPedido.EN_REPARTO -> android.graphics.Color.parseColor("#FF9800")
            EstadoPedido.ENTREGADO -> android.graphics.Color.parseColor("#4CAF50")
        }
    }

    fun getEstadoTexto(): String {
        return when (estado) {
            EstadoPedido.PENDIENTE -> "Pendiente"
            EstadoPedido.PREPARADO -> "Preparado"
            EstadoPedido.EN_REPARTO -> "En Reparto"
            EstadoPedido.ENTREGADO -> "Entregado"
        }
    }

    fun getEstadoSiguiente(): EstadoPedido? {
        return when (estado) {
            EstadoPedido.PENDIENTE -> EstadoPedido.PREPARADO
            EstadoPedido.PREPARADO -> EstadoPedido.EN_REPARTO
            EstadoPedido.EN_REPARTO -> EstadoPedido.ENTREGADO
            EstadoPedido.ENTREGADO -> null // Ya completado
        }
    }

    fun getTextoBotonSiguienteEstado(): String? {
        return when (estado) {
            EstadoPedido.PENDIENTE -> "Marcar como Preparado"
            EstadoPedido.PREPARADO -> "Iniciar Reparto"
            EstadoPedido.EN_REPARTO -> "Confirmar Entrega"
            EstadoPedido.ENTREGADO -> null
        }
    }
}

enum class EstadoPedido {
    PENDIENTE,
    PREPARADO,
    EN_REPARTO,
    ENTREGADO;

    companion object {
        fun fromString(estado: String): EstadoPedido {
            return when (estado.lowercase()) {
                "pendiente" -> PENDIENTE
                "preparado" -> PREPARADO
                "en_reparto" -> EN_REPARTO
                "entregado" -> ENTREGADO
                else -> PENDIENTE
            }
        }
    }
}

// Extensión para convertir Response a modelo de dominio
fun PedidoResponse.toDomain(): Pedido {
    return Pedido(
        id = idPedido,
        idCliente = idCliente,
        idRepartidor = idRepartidor,
        estado = EstadoPedido.fromString(estado),
        fechaPedido = fechaPedido,
        fechaEntrega = fechaEntrega
    )
}