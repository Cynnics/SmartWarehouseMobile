package com.smartwarehouse.mobile.data.local.dao

import androidx.room.*
import com.smartwarehouse.mobile.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM producto WHERE activo = 1 ORDER BY nombre ASC")
    fun getAllProductos(): Flow<List<ProductoEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducto(producto: ProductoEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductos(productos: List<ProductoEntity>)
    @Query("SELECT COUNT(*) FROM producto")
    suspend fun getProductosCount(): Int
}

@Dao
interface PedidoDao {
    @Query("SELECT * FROM pedido ORDER BY fechaPedido DESC")
    fun getAllPedidos(): Flow<List<PedidoEntity>>
    @Query("SELECT * FROM pedido WHERE idPedido = :id")
    suspend fun getPedidoById(id: Int): PedidoEntity?
    @Query("SELECT * FROM pedido WHERE estado = :estado ORDER BY fechaPedido DESC")
    fun getPedidosByEstado(estado: String): Flow<List<PedidoEntity>>
    @Query("SELECT * FROM pedido WHERE idCliente = :idCliente ORDER BY fechaPedido DESC")
    fun getPedidosByCliente(idCliente: Int): Flow<List<PedidoEntity>>
    @Query("SELECT * FROM pedido WHERE idRepartidor = :idRepartidor ORDER BY fechaPedido DESC")
    fun getPedidosByRepartidor(idRepartidor: Int): Flow<List<PedidoEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPedido(pedido: PedidoEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPedidos(pedidos: List<PedidoEntity>)
    @Update
    suspend fun updatePedido(pedido: PedidoEntity)
    @Query("SELECT COUNT(*) FROM pedido")
    suspend fun getPedidosCount(): Int
}

@Dao
interface DetallePedidoDao {
    @Query("SELECT * FROM detallepedido WHERE idPedido = :idPedido")
    fun getDetallesByPedido(idPedido: Int): Flow<List<DetallePedidoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetalles(detalles: List<DetallePedidoEntity>)
}

@Dao
interface RutaDao {
    @Query("SELECT * FROM rutaentrega ORDER BY fechaRuta DESC")
    fun getAllRutas(): Flow<List<RutaEntity>>

    @Query("SELECT * FROM rutaentrega WHERE idRepartidor = :idRepartidor ORDER BY fechaRuta DESC")
    fun getRutasByRepartidor(idRepartidor: Int): Flow<List<RutaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRuta(ruta: RutaEntity)

    @Update
    suspend fun updateRuta(ruta: RutaEntity)
    @Query("SELECT * FROM rutaentrega WHERE idRuta = :id")
    suspend fun getRutaById(id: Int): RutaEntity?
}

@Dao
interface UbicacionDao {
    @Query("SELECT * FROM ubicacionrepartidor WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun getUbicacionesNoSincronizadas(): List<UbicacionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUbicacion(ubicacion: UbicacionEntity): Long

    @Query("UPDATE ubicacionrepartidor SET synced = 1 WHERE id = :id")
    suspend fun marcarComoSincronizada(id: Int)
}