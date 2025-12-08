package com.smartwarehouse.mobile.data.api

import com.smartwarehouse.mobile.data.model.request.ActualizarUsuarioRequest
import com.smartwarehouse.mobile.data.model.response.UsuarioResponse
import retrofit2.Response
import retrofit2.http.*

interface UsuarioService {

    @GET("Usuarios")
    suspend fun getUsuarios(): Response<List<UsuarioResponse>>

    @GET("Repartidores")
    suspend fun getRepartidores(): Response<List<UsuarioResponse>>

    @PATCH("Usuarios/{id}")
    suspend fun actualizarUsuario(
        @Path("id") idUsuario: Int,
        @Body request: ActualizarUsuarioRequest
    ): Response<UsuarioResponse>
}