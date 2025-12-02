package com.smartwarehouse.mobile.data.model

import com.smartwarehouse.mobile.data.model.response.ProductoResponse

// Item del carrito (producto + cantidad)
data class ItemCarrito(
    val producto: ProductoResponse,
    var cantidad: Int = 1
) {
    fun getSubtotal(): Double = producto.precio * cantidad

    fun incrementar() {
        if (cantidad < producto.stock) {
            cantidad++
        }
    }

    fun decrementar() {
        if (cantidad > 1) {
            cantidad--
        }
    }
}

// Modelo del carrito completo
data class Carrito(
    val items: MutableList<ItemCarrito> = mutableListOf()
) {
    fun agregarProducto(producto: ProductoResponse) {
        val itemExistente = items.find { it.producto.idProducto == producto.idProducto }

        if (itemExistente != null) {
            itemExistente.incrementar()
        } else {
            items.add(ItemCarrito(producto, 1))
        }
    }

    fun eliminarProducto(idProducto: Int) {
        items.removeAll { it.producto.idProducto == idProducto }
    }

    fun actualizarCantidad(idProducto: Int, nuevaCantidad: Int) {
        items.find { it.producto.idProducto == idProducto }?.let {
            if (nuevaCantidad > 0 && nuevaCantidad <= it.producto.stock) {
                it.cantidad = nuevaCantidad
            }
        }
    }

    fun vaciar() {
        items.clear()
    }

    fun getSubtotal(): Double = items.sumOf { it.getSubtotal() }

    fun getIVA(): Double = getSubtotal() * 0.21

    fun getTotal(): Double = getSubtotal() + getIVA()

    fun getTotalItems(): Int = items.sumOf { it.cantidad }

    fun isEmpty(): Boolean = items.isEmpty()
}

// Request para crear pedido
data class CrearPedidoRequest(
    val idCliente: Int,
    val items: List<ItemPedidoRequest>,
    val direccionEntrega: String? = null,
    val notas: String? = null,
    val estado: String
)

data class ItemPedidoRequest(
    val idProducto: Int,
    val cantidad: Int,
    val subtotal: Double
)