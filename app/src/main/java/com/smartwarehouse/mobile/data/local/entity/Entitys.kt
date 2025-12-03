package com.smartwarehouse.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.smartwarehouse.mobile.data.local.converters.DateConverter

@Entity(tableName = "productos")
data class ProductoEntity(
    @PrimaryKey
    val idProducto: Int,
    val nombre: String,
    val descripcion: String?,
    val precio: Double,
    val stock: Int,
    val categoria: String?,
    val activo: Boolean,
    val lastSync: Long = System.currentTimeMillis()
)

@Entity(tableName = "pedidos")
@TypeConverters(DateConverter::class)
data class PedidoEntity(
    @PrimaryKey
    val idPedido: Int,
    val idCliente: Int,
    val idRepartidor: Int?,
    val estado: String,
    val fechaPedido: String,
    val fechaEntrega: String?,
    val nombreCliente: String?,
    val direccionEntrega: String?,
    val telefonoCliente: String?,
    val lastSync: Long = System.currentTimeMillis()
)

@Entity(tableName = "detalle_pedido")
data class DetallePedidoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val idDetalle: Int,
    val idPedido: Int,
    val idProducto: Int,
    val cantidad: Int,
    val subtotal: Double,
    val lastSync: Long = System.currentTimeMillis()
)

@Entity(tableName = "rutas")
data class RutaEntity(
    @PrimaryKey
    val idRuta: Int,
    val idRepartidor: Int,
    val fechaRuta: String,
    val distanciaEstimadaKm: Double?,
    val duracionEstimadaMin: Int?,
    val estado: String,
    val lastSync: Long = System.currentTimeMillis()
)

@Entity(tableName = "ubicaciones")
data class UbicacionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val idUbicacion: Int?,
    val idRepartidor: Int,
    val latitud: Double,
    val longitud: Double,
    val fechaHora: String,
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)