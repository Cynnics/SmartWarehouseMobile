package com.smartwarehouse.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.smartwarehouse.mobile.data.local.converters.DateConverter
import com.smartwarehouse.mobile.domain.model.Usuario

@Entity(tableName = "producto")
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

@Entity(tableName = "pedido")
data class PedidoEntity(
    @PrimaryKey
    val idPedido: Int,
    val idCliente: Int,
    val idRepartidor: Int?,
    val estado: String,
    val fechaPedido: String,
    val fechaEntrega: String?,
    val direccionEntrega: String?,
    val ciudad: String?,
    val codigoPostal: String?,
    val latitud: Double?,
    val longitud: Double?,
    val nombreCliente: String?,
    val telefonoCliente: String?,
    val lastSync: Long = System.currentTimeMillis()
)

@Entity(tableName = "detallepedido")
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

@Entity(tableName = "rutaentrega")
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

@Entity(tableName = "ubicacionrepartidor")
data class UbicacionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val idUbicacion: Int?,
    val idRepartidor: Int,
    val latitud: Double,
    val longitud: Double,
    val fechaHora: String?,
    val synced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "usuario")
data class UsuarioEntity (
    @PrimaryKey(autoGenerate = true)
    val idUsuario: Int,
    val nombre : String,
    val email : String,
    val rol : String,
    val telefono : String

)

@Entity(tableName = "rutapedido", primaryKeys = ["idRuta", "idPedido"])
data class RutaPedidoEntity(
    val idRuta: Int,
    val idPedido: Int
)
