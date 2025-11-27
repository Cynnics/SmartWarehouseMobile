package com.smartwarehouse.mobile.data.api

import com.smartwarehouse.mobile.data.model.request.LoginRequest
import com.smartwarehouse.mobile.data.model.response.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("Usuarios/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    // Puedes añadir más endpoints relacionados con autenticación
    // @POST("Usuarios/register")
    // suspend fun register(@Body registerRequest: RegisterRequest): Response<RegisterResponse>
}