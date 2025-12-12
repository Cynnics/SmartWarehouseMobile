package com.smartwarehouse.mobile.data.api

import com.smartwarehouse.mobile.data.model.response.*
import retrofit2.Response
import retrofit2.http.*

interface RutaService {

    @GET("Rutas")
    suspend fun getRutas(): Response<List<RutaEntregaResponse>>

    @GET("Rutas/{id}")
    suspend fun getRutaById(@Path("id") idRuta: Int): Response<RutaEntregaResponse>

    @POST("Rutas")
    suspend fun crearRuta(@Body ruta: ActualizarRutaRequest): Response<RutaEntregaResponse>

    @PATCH("Rutas/{id}")
    suspend fun actualizarRuta(
        @Path("id") idRuta: Int,
        @Body ruta: ActualizarRutaRequest
    ): Response<RutaEntregaResponse>

    @PATCH("Rutas/{id}/estado")
    suspend fun cambiarEstadoRuta(
        @Path("id") idRuta: Int,
        @Body nuevoEstado: String
    ): Response<Any>

    @POST("Rutas/{id}/pedidos/{pedidoId}")
    suspend fun asignarPedidoARuta(
        @Path("id") idRuta: Int,
        @Path("pedidoId") pedidoId: Int
    ): Response<Any>

    @GET("Rutas/{id}/pedidos")
    suspend fun getPedidosDeRuta(@Path("id") idRuta: Int): Response<List<PedidoResponse>>

    @GET("Rutas/{id}/ubicaciones")
    suspend fun getUbicacionesDeRuta(@Path("id") idRuta: Int): Response<List<UbicacionRepartidorResponse>>

    @DELETE("Rutas/{id}")
    suspend fun eliminarRuta(@Path("id") idRuta: Int): Response<Unit>
}

interface UbicacionService {

    @GET("UbicacionesRepartidor")
    suspend fun getUbicaciones(): Response<List<UbicacionRepartidorResponse>>

    @GET("UbicacionesRepartidor/repartidor/{idRepartidor}")
    suspend fun getUbicacionesByRepartidor(
        @Path("idRepartidor") idRepartidor: Int
    ): Response<List<UbicacionRepartidorResponse>>

    @GET("UbicacionesRepartidor/last/repartidor/{idRepartidor}")
    suspend fun getUltimaUbicacion(
        @Path("idRepartidor") idRepartidor: Int
    ): Response<UbicacionRepartidorResponse>

    @POST("UbicacionesRepartidor")
    suspend fun crearUbicacion(
        @Body ubicacion: CrearUbicacionRequest
    ): Response<UbicacionRepartidorResponse>

    @DELETE("UbicacionesRepartidor/{id}")
    suspend fun eliminarUbicacion(@Path("id") idUbicacion: Int): Response<Unit>
}