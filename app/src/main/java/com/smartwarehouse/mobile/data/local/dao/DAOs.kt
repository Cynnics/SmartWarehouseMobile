package com.smartwarehouse.mobile.data.local.dao

import androidx.room.*
import com.smartwarehouse.mobile.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos WHERE activo = 1 ORDER BY nombre ASC")
    fun getAllProductos(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE idProducto = :id")
    suspend fun getProductoById(id: Int): ProductoEntity?

    @Query("SELECT * FROM productos WHERE nombre LIKE '%' || :query || '%' OR categoria LIKE '%' || :query || '%'")
    fun searchProductos(query: String): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM productos WHERE categoria = :categoria AND activo = 1")
    fun getProductosByCategoria(categoria: String): Flow<List<ProductoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducto(producto: ProductoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductos(productos: List<ProductoEntity>)

    @Update
    suspend fun updateProducto(producto: ProductoEntity)

    @Delete
    suspend fun deleteProducto(producto: ProductoEntity)

    @Query("DELETE FROM productos")
    suspend fun deleteAllProductos()

    @Query("SELECT COUNT(*) FROM productos")
    suspend fun getProductosCount(): Int
}

@Dao
interface PedidoDao {
    @Query("SELECT * FROM pedidos ORDER BY fechaPedido DESC")
    fun getAllPedidos(): Flow<List<PedidoEntity>>

    @Query("SELECT * FROM pedidos WHERE idPedido = :id")
    suspend fun getPedidoById(id: Int): PedidoEntity?

    @Query("SELECT * FROM pedidos WHERE estado = :estado ORDER BY fechaPedido DESC")
    fun getPedidosByEstado(estado: String): Flow<List<PedidoEntity>>

    @Query("SELECT * FROM pedidos WHERE idCliente = :idCliente ORDER BY fechaPedido DESC")
    fun getPedidosByCliente(idCliente: Int): Flow<List<PedidoEntity>>

    @Query("SELECT * FROM pedidos WHERE idRepartidor = :idRepartidor ORDER BY fechaPedido DESC")
    fun getPedidosByRepartidor(idRepartidor: Int): Flow<List<PedidoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPedido(pedido: PedidoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPedidos(pedidos: List<PedidoEntity>)

    @Update
    suspend fun updatePedido(pedido: PedidoEntity)

    @Delete
    suspend fun deletePedido(pedido: PedidoEntity)

    @Query("DELETE FROM pedidos")
    suspend fun deleteAllPedidos()

    @Query("SELECT COUNT(*) FROM pedidos")
    suspend fun getPedidosCount(): Int
}

@Dao
interface DetallePedidoDao {
    @Query("SELECT * FROM detalle_pedido WHERE idPedido = :idPedido")
    fun getDetallesByPedido(idPedido: Int): Flow<List<DetallePedidoEntity>>

    @Query("SELECT * FROM detalle_pedido WHERE idPedido = :idPedido")
    suspend fun getDetallesByPedidoSync(idPedido: Int): List<DetallePedidoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetalle(detalle: DetallePedidoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetalles(detalles: List<DetallePedidoEntity>)

    @Query("DELETE FROM detalle_pedido WHERE idPedido = :idPedido")
    suspend fun deleteDetallesByPedido(idPedido: Int)

    @Query("DELETE FROM detalle_pedido")
    suspend fun deleteAllDetalles()
}

@Dao
interface RutaDao {
    @Query("SELECT * FROM rutas ORDER BY fechaRuta DESC")
    fun getAllRutas(): Flow<List<RutaEntity>>

    @Query("SELECT * FROM rutas WHERE idRuta = :id")
    suspend fun getRutaById(id: Int): RutaEntity?

    @Query("SELECT * FROM rutas WHERE idRepartidor = :idRepartidor ORDER BY fechaRuta DESC")
    fun getRutasByRepartidor(idRepartidor: Int): Flow<List<RutaEntity>>

    @Query("SELECT * FROM rutas WHERE estado = :estado ORDER BY fechaRuta DESC")
    fun getRutasByEstado(estado: String): Flow<List<RutaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRuta(ruta: RutaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRutas(rutas: List<RutaEntity>)

    @Update
    suspend fun updateRuta(ruta: RutaEntity)

    @Delete
    suspend fun deleteRuta(ruta: RutaEntity)

    @Query("DELETE FROM rutas")
    suspend fun deleteAllRutas()
}

@Dao
interface UbicacionDao {
    @Query("SELECT * FROM ubicaciones ORDER BY createdAt DESC")
    fun getAllUbicaciones(): Flow<List<UbicacionEntity>>

    @Query("SELECT * FROM ubicaciones WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun getUbicacionesNoSincronizadas(): List<UbicacionEntity>

    @Query("SELECT * FROM ubicaciones WHERE idRepartidor = :idRepartidor ORDER BY createdAt DESC LIMIT 1")
    suspend fun getUltimaUbicacion(idRepartidor: Int): UbicacionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUbicacion(ubicacion: UbicacionEntity): Long

    @Update
    suspend fun updateUbicacion(ubicacion: UbicacionEntity)

    @Query("UPDATE ubicaciones SET synced = 1 WHERE id = :id")
    suspend fun marcarComoSincronizada(id: Int)

    @Query("DELETE FROM ubicaciones WHERE createdAt < :timestamp")
    suspend fun deleteUbicacionesAntiguas(timestamp: Long)

    @Query("DELETE FROM ubicaciones")
    suspend fun deleteAllUbicaciones()
}
