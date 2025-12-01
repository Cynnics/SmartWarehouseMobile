package com.smartwarehouse.mobile.data.api

import com.smartwarehouse.mobile.data.model.response.*
import retrofit2.Response
import retrofit2.http.*

interface RutaService {

    // GET: Obtener todas las rutas
    @GET("Rutas")
    suspend fun getRutas(): Response<List<RutaEntregaResponse>>

    // GET: Obtener una ruta por ID
    @GET("Rutas/{id}")
    suspend fun getRutaById(@Path("id") idRuta: Int): Response<RutaEntregaResponse>

    // POST: Crear nueva ruta
    @POST("Rutas")
    suspend fun crearRuta(@Body ruta: RutaEntregaResponse): Response<RutaEntregaResponse>

    // PATCH: Actualizar ruta
    @PATCH("Rutas/{id}")
    suspend fun actualizarRuta(
        @Path("id") idRuta: Int,
        @Body ruta: ActualizarRutaRequest
    ): Response<RutaEntregaResponse>

    // PATCH: Cambiar estado de la ruta
    @PATCH("Rutas/{id}/estado")
    suspend fun cambiarEstadoRuta(
        @Path("id") idRuta: Int,
        @Body nuevoEstado: String
    ): Response<Any>

    // POST: Asignar pedido a ruta
    @POST("Rutas/{id}/pedidos/{pedidoId}")
    suspend fun asignarPedidoARuta(
        @Path("id") idRuta: Int,
        @Path("pedidoId") pedidoId: Int
    ): Response<Any>

    // GET: Obtener pedidos de una ruta
    @GET("Rutas/{id}/pedidos")
    suspend fun getPedidosDeRuta(@Path("id") idRuta: Int): Response<List<PedidoResponse>>

    // GET: Obtener ubicaciones del repartidor de la ruta
    @GET("Rutas/{id}/ubicaciones")
    suspend fun getUbicacionesDeRuta(@Path("id") idRuta: Int): Response<List<UbicacionRepartidorResponse>>

    // DELETE: Eliminar ruta
    @DELETE("Rutas/{id}")
    suspend fun eliminarRuta(@Path("id") idRuta: Int): Response<Unit>
}

interface UbicacionService {

    // GET: Obtener todas las ubicaciones
    @GET("UbicacionesRepartidor")
    suspend fun getUbicaciones(): Response<List<UbicacionRepartidorResponse>>

    // GET: Obtener ubicaciones por repartidor
    @GET("UbicacionesRepartidor/repartidor/{idRepartidor}")
    suspend fun getUbicacionesByRepartidor(
        @Path("idRepartidor") idRepartidor: Int
    ): Response<List<UbicacionRepartidorResponse>>

    // GET: Obtener última ubicación del repartidor
    @GET("UbicacionesRepartidor/last/repartidor/{idRepartidor}")
    suspend fun getUltimaUbicacion(
        @Path("idRepartidor") idRepartidor: Int
    ): Response<UbicacionRepartidorResponse>

    // POST: Crear nueva ubicación (tracking GPS)
    @POST("UbicacionesRepartidor")
    suspend fun crearUbicacion(
        @Body ubicacion: CrearUbicacionRequest
    ): Response<UbicacionRepartidorResponse>

    // DELETE: Eliminar ubicación
    @DELETE("UbicacionesRepartidor/{id}")
    suspend fun eliminarUbicacion(@Path("id") idUbicacion: Int): Response<Unit>
}