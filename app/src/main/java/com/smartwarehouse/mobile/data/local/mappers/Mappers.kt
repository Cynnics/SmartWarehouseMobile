package com.smartwarehouse.mobile.data.local.mappers

import com.smartwarehouse.mobile.data.local.entity.*
import com.smartwarehouse.mobile.data.model.response.*

// Producto
fun ProductoResponse.toEntity(): ProductoEntity {
    return ProductoEntity(
        idProducto = this.idProducto,
        nombre = this.nombre,
        descripcion = this.descripcion,
        precio = this.precio,
        stock = this.stock,
        categoria = this.categoria,
        activo = this.activo
    )
}

fun ProductoEntity.toResponse(): ProductoResponse {
    return ProductoResponse(
        idProducto = this.idProducto,
        nombre = this.nombre,
        descripcion = this.descripcion,
        precio = this.precio,
        stock = this.stock,
        categoria = this.categoria,
        activo = this.activo
    )
}

// Pedido
fun PedidoResponse.toEntity(): PedidoEntity {
    return PedidoEntity(
        idPedido = this.idPedido,
        idCliente = this.idCliente,
        idRepartidor = this.idRepartidor,
        estado = this.estado,
        fechaPedido = this.fechaPedido,
        fechaEntrega = this.fechaEntrega,
        direccionEntrega = this.direccionEntrega,

        // 🔥 NUEVOS CAMPOS
        ciudad = this.ciudad,
        codigoPostal = this.codigoPostal,
        latitud = this.latitud,
        longitud = this.longitud,

        nombreCliente = null,
        telefonoCliente = null
    )
}

fun PedidoEntity.toResponse(): PedidoResponse {
    return PedidoResponse(
        idPedido = this.idPedido,
        idCliente = this.idCliente,
        idRepartidor = this.idRepartidor,
        estado = this.estado,
        fechaPedido = this.fechaPedido,
        fechaEntrega = this.fechaEntrega,
        direccionEntrega = this.direccionEntrega,

        // 🔥 NUEVOS CAMPOS
        ciudad = this.ciudad,
        codigoPostal = this.codigoPostal,
        latitud = this.latitud,
        longitud = this.longitud,

        notas = null
    )
}

fun PedidoEntity.toDomain(): Pedido {
    return Pedido(
        id = this.idPedido,
        idCliente = this.idCliente,
        idRepartidor = this.idRepartidor,
        estado = EstadoPedido.fromString(this.estado),
        fechaPedido = this.fechaPedido,
        fechaEntrega = this.fechaEntrega,
        direccionEntrega = this.direccionEntrega,

        // 🔥 NUEVOS CAMPOS
        ciudad = this.ciudad,
        codigoPostal = this.codigoPostal,
        latitud = this.latitud,
        longitud = this.longitud,

        nombreCliente = this.nombreCliente,
        telefonoCliente = this.telefonoCliente
    )
}
fun Pedido.toEntity(): PedidoEntity {
    return PedidoEntity(
        idPedido = this.id,
        idCliente = this.idCliente,
        idRepartidor = this.idRepartidor,
        estado = this.estado.name.lowercase(),
        fechaPedido = this.fechaPedido,
        fechaEntrega = this.fechaEntrega,
        direccionEntrega = this.direccionEntrega,

        // 🔥 NUEVOS CAMPOS
        ciudad = this.ciudad,
        codigoPostal = this.codigoPostal,
        latitud = this.latitud,
        longitud = this.longitud,

        nombreCliente = this.nombreCliente,
        telefonoCliente = this.telefonoCliente
    )
}

// Ruta
fun RutaEntregaResponse.toEntity(): RutaEntity {
    return RutaEntity(
        idRuta = this.idRuta,
        idRepartidor = this.idRepartidor,
        fechaRuta = this.fechaRuta,
        distanciaEstimadaKm = this.distanciaEstimadaKm,
        duracionEstimadaMin = this.duracionEstimadaMin,
        estado = this.estado ?: "pendiente"
    )
}

fun RutaEntity.toResponse(): RutaEntregaResponse {
    return RutaEntregaResponse(
        idRuta = this.idRuta,
        idRepartidor = this.idRepartidor,
        fechaRuta = this.fechaRuta,
        distanciaEstimadaKm = this.distanciaEstimadaKm,
        duracionEstimadaMin = this.duracionEstimadaMin,
        estado = this.estado
    )
}

fun RutaEntity.toDomain(): Ruta {
    return Ruta(
        id = this.idRuta,
        idRepartidor = this.idRepartidor,
        fechaRuta = this.fechaRuta,
        distanciaEstimadaKm = this.distanciaEstimadaKm,
        duracionEstimadaMin = this.duracionEstimadaMin,
        estado = EstadoRuta.fromString(this.estado)
    )
}

// Ubicación
fun UbicacionRepartidorResponse.toEntity(idRepartidor: Int): UbicacionEntity {
    return UbicacionEntity(
        idUbicacion = this.idUbicacion,
        idRepartidor = idRepartidor,
        latitud = this.latitud,
        longitud = this.longitud,
        fechaHora = this.fechaHora?.takeIf { it.isNotBlank() }, // <- convierte "" en null
        synced = true
    )
}



fun Ruta.toEntity(): RutaEntity {
    return RutaEntity(
        idRuta = this.id,
        idRepartidor = this.idRepartidor,
        fechaRuta = this.fechaRuta,
        distanciaEstimadaKm = this.distanciaEstimadaKm,
        duracionEstimadaMin = this.duracionEstimadaMin,
        estado = this.estado.name.lowercase()
    )
}