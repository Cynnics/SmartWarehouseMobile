package com.smartwarehouse.mobile.data.model.response

import com.google.gson.annotations.SerializedName

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

data class CrearUbicacionRequest(
    val idRepartidor: Int,
    val latitud: Double,
    val longitud: Double,
    val fechaHora: String? = null
)

data class ActualizarRutaRequest(
    val idRepartidor: Int?,
    val fechaRuta: String?,
    val distanciaEstimadaKm: Double?,
    val duracionEstimadaMin: Int?,
    val estado: String?
)

data class Ruta(
    val id: Int,
    val idRepartidor: Int,
    val fechaRuta: String,
    val distanciaEstimadaKm: Double?,
    val duracionEstimadaMin: Int?,
    val estado: EstadoRuta
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

