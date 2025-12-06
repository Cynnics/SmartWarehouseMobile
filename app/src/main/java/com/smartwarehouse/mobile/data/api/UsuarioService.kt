package com.smartwarehouse.mobile.data.api

import com.smartwarehouse.mobile.data.model.response.UsuarioResponse
import retrofit2.Response
import retrofit2.http.GET

interface UsuarioService {

    @GET("Usuarios")
    suspend fun getUsuarios(): Response<List<UsuarioResponse>>

    @GET("Repartidores")
    suspend fun getRepartidores(): Response<List<UsuarioResponse>>
}