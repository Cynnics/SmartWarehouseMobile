package com.smartwarehouse.mobile.data.model.response

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName

// Modelo de respuesta de la API
data class RutaEntregaResponse(
    @SerializedName("idRuta")
    val idRuta: Int,

    @SerializedName("idRepartidor")
    val idRepartidor: Int,

    @SerializedName("fechaRuta")
    val fechaRuta: String, // "2024-11-30T00:00:00"

    @SerializedName("distanciaEstimadaKm")
    val distanciaEstimadaKm: Double?,

    @SerializedName("duracionEstimadaMin")
    val duracionEstimadaMin: Int?,

    @SerializedName("estado")
    val estado: String? // "pendiente", "en_curso", "completada"
)

// Modelo de ubicación del repartidor
data class UbicacionRepartidorResponse(
    @SerializedName("idUbicacion")
    val idUbicacion: Int,

    @SerializedName("idRepartidor")
    val idRepartidor: Int,

    @SerializedName("latitud")
    val latitud: Double,

    @SerializedName("longitud")
    val longitud: Double,

    @SerializedName("fechaHora")
    val fechaHora: String?=null
)

// Request para crear ubicación
data class CrearUbicacionRequest(
    val idRepartidor: Int,
    val latitud: Double,
    val longitud: Double,
    val fechaHora: String? = null // Se enviará vacío, la API usa DateTime.Now
)

// Request para actualizar ruta
data class ActualizarRutaRequest(
    val idRepartidor: Int,
    val fechaRuta: String,
    val distanciaEstimadaKm: Double?,
    val duracionEstimadaMin: Int?,
    val estado: String?
)

// Modelo de dominio para la UI
data class Ruta(
    val id: Int,
    val idRepartidor: Int,
    val fechaRuta: String,
    val distanciaEstimadaKm: Double?,
    val duracionEstimadaMin: Int?,
    val estado: EstadoRuta,
    val pedidos: List<PedidoConDireccion> = emptyList()
) {
    fun getEstadoColor(): Int {
        return when (estado) {
            EstadoRuta.PENDIENTE -> android.graphics.Color.parseColor("#FFC107")
            EstadoRuta.EN_CURSO -> android.graphics.Color.parseColor("#2196F3")
            EstadoRuta.COMPLETADA -> android.graphics.Color.parseColor("#4CAF50")
        }
    }

    fun getEstadoTexto(): String {
        return when (estado) {
            EstadoRuta.PENDIENTE -> "Pendiente"
            EstadoRuta.EN_CURSO -> "En Curso"
            EstadoRuta.COMPLETADA -> "Completada"
        }
    }

    fun getDistanciaTexto(): String {
        return distanciaEstimadaKm?.let { String.format("%.1f km", it) } ?: "-- km"
    }

    fun getDuracionTexto(): String {
        return duracionEstimadaMin?.let { "$it min" } ?: "-- min"
    }
}

enum class EstadoRuta {
    PENDIENTE,
    EN_CURSO,
    COMPLETADA;

    companion object {
        fun fromString(estado: String?): EstadoRuta {
            return when (estado?.lowercase()) {
                "pendiente" -> PENDIENTE
                "en_curso", "en curso" -> EN_CURSO
                "completada" -> COMPLETADA
                else -> PENDIENTE
            }
        }
    }
}

// Modelo para pedidos con dirección (para el mapa)
data class PedidoConDireccion(
    val idPedido: Int,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val estado: String,
    val nombreCliente: String? = null
) {
    fun toLatLng(): LatLng = LatLng(latitud, longitud)
}

// Extensión para convertir Response a modelo de dominio
fun RutaEntregaResponse.toDomain(): Ruta {
    return Ruta(
        id = idRuta,
        idRepartidor = idRepartidor,
        fechaRuta = fechaRuta,
        distanciaEstimadaKm = distanciaEstimadaKm,
        duracionEstimadaMin = duracionEstimadaMin,
        estado = EstadoRuta.fromString(estado)
    )
}

fun UbicacionRepartidorResponse.toLatLng(): LatLng {
    return LatLng(latitud, longitud)
}