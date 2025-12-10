package com.smartwarehouse.mobile.data.api

import com.smartwarehouse.mobile.data.model.response.CrearPedidoRequest
import com.smartwarehouse.mobile.data.model.response.DetallePedidoResponse
import com.smartwarehouse.mobile.data.model.response.PedidoResponse
import com.smartwarehouse.mobile.data.model.response.ProductoResponse
import com.smartwarehouse.mobile.data.model.response.TotalesPedidoResponse
import com.smartwarehouse.mobile.data.model.response.UsuarioResponse
import retrofit2.Response
import retrofit2.http.*

interface PedidoService {

    // GET: Obtener todos los pedidos
    @GET("Pedidos")
    suspend fun getPedidos(): Response<List<PedidoResponse>>

    // GET: Obtener pedidos con filtro de estado
    @GET("Pedidos")
    suspend fun getPedidosByEstado(@Query("estado") estado: String): Response<List<PedidoResponse>>

    // GET: Obtener pedidos entregados
    @GET("Pedidos/entregados")
    suspend fun getPedidosEntregados(): Response<List<PedidoResponse>>

    // GET: Obtener un pedido por ID
    @GET("Pedidos/{id}")
    suspend fun getPedidoById(@Path("id") idPedido: Int): Response<PedidoResponse>

    // GET: Obtener totales de un pedido
    @GET("Pedidos/{id}/totales")
    suspend fun getTotalesPedido(@Path("id") idPedido: Int): Response<TotalesPedidoResponse>

    // PATCH: Cambiar estado del pedido
    @PATCH("Pedidos/{id}/estado")
    suspend fun cambiarEstado(
        @Path("id") idPedido: Int,
        @Body nuevoEstado: String
    ): Response<Any>

    // POST: Crear pedido (para clientes)
    @POST("Pedidos")
    suspend fun crearPedido(@Body pedido: CrearPedidoRequest): Response<PedidoResponse>

    // PUT: Actualizar pedido
    @PUT("Pedidos/{id}")
    suspend fun actualizarPedido(
        @Path("id") idPedido: Int,
        @Body pedido: PedidoResponse
    ): Response<PedidoResponse>

    // DELETE: Eliminar pedido (solo admin)
    @DELETE("Pedidos/{id}")
    suspend fun eliminarPedido(@Path("id") idPedido: Int): Response<Unit>

    // GET: Obtener detalles de un pedido
    @GET("DetallePedido/pedido/{idPedido}")
    suspend fun getDetallesPedido(@Path("idPedido") idPedido: Int): Response<List<DetallePedidoResponse>>

    // POST: Crear detallePedido
    @POST("DetallePedido")
    suspend fun crearDetallePedido(@Body detalle: DetallePedidoResponse) : Response<DetallePedidoResponse>

    @GET("/api/Usuarios/{id}")
    suspend fun getUsuarioById(@Path("id") id: Int): Response<UsuarioResponse>

    // GET: Obtener un producto por ID
    @GET("Productos/{id}")
    suspend fun getProductoById(@Path("id") idProducto: Int): Response<ProductoResponse>
}
