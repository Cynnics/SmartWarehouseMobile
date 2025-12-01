package com.smartwarehouse.mobile.data.api

import com.smartwarehouse.mobile.data.model.response.ProductoResponse
import retrofit2.Response
import retrofit2.http.*

interface ProductoService {

    @GET("Productos")
    suspend fun getProductos(): Response<List<ProductoResponse>>

    @GET("Productos/{id}")
    suspend fun getProductoById(@Path("id") idProducto: Int): Response<ProductoResponse>

    @POST("Productos")
    suspend fun crearProducto(@Body producto: ProductoResponse): Response<ProductoResponse>

    @PATCH("Productos/{id}")
    suspend fun actualizarProducto(
        @Path("id") idProducto: Int,
        @Body producto: ProductoResponse
    ): Response<ProductoResponse>

    @DELETE("Productos/{id}")
    suspend fun eliminarProducto(@Path("id") idProducto: Int): Response<Unit>
}