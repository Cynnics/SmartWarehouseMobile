package com.smartwarehouse.mobile.data.local.dao

import androidx.room.*
import com.smartwarehouse.mobile.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM producto WHERE activo = 1 ORDER BY nombre ASC")
    fun getAllProductos(): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM producto WHERE idProducto = :id")
    suspend fun getProductoById(id: Int): ProductoEntity?

    @Query("SELECT * FROM producto WHERE nombre LIKE '%' || :query || '%' OR categoria LIKE '%' || :query || '%'")
    fun searchProductos(query: String): Flow<List<ProductoEntity>>

    @Query("SELECT * FROM producto WHERE categoria = :categoria AND activo = 1")
    fun getProductosByCategoria(categoria: String): Flow<List<ProductoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducto(producto: ProductoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductos(productos: List<ProductoEntity>)

    @Update
    suspend fun updateProducto(producto: ProductoEntity)

    @Delete
    suspend fun deleteProducto(producto: ProductoEntity)

    @Query("DELETE FROM producto")
    suspend fun deleteAllProductos()

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

    @Delete
    suspend fun deletePedido(pedido: PedidoEntity)

    @Query("DELETE FROM pedido")
    suspend fun deleteAllPedidos()

    @Query("SELECT COUNT(*) FROM pedido")
    suspend fun getPedidosCount(): Int
}

@Dao
interface DetallePedidoDao {
    @Query("SELECT * FROM detallepedido WHERE idPedido = :idPedido")
    fun getDetallesByPedido(idPedido: Int): Flow<List<DetallePedidoEntity>>

    @Query("SELECT * FROM detallepedido WHERE idPedido = :idPedido")
    suspend fun getDetallesByPedidoSync(idPedido: Int): List<DetallePedidoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetalle(detalle: DetallePedidoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetalles(detalles: List<DetallePedidoEntity>)

    @Query("DELETE FROM detallepedido WHERE idPedido = :idPedido")
    suspend fun deleteDetallesByPedido(idPedido: Int)

    @Query("DELETE FROM detallepedido")
    suspend fun deleteAllDetalles()
}

@Dao
interface RutaDao {
    @Query("SELECT * FROM rutaentrega ORDER BY fechaRuta DESC")
    fun getAllRutas(): Flow<List<RutaEntity>>

    @Query("SELECT * FROM rutaentrega WHERE idRuta = :id")
    suspend fun getRutaById(id: Int): RutaEntity?

    @Query("SELECT * FROM rutaentrega WHERE idRepartidor = :idRepartidor ORDER BY fechaRuta DESC")
    fun getRutasByRepartidor(idRepartidor: Int): Flow<List<RutaEntity>>

    @Query("SELECT * FROM rutaentrega WHERE estado = :estado ORDER BY fechaRuta DESC")
    fun getRutasByEstado(estado: String): Flow<List<RutaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRuta(ruta: RutaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRutas(rutas: List<RutaEntity>)

    @Update
    suspend fun updateRuta(ruta: RutaEntity)

    @Delete
    suspend fun deleteRuta(ruta: RutaEntity)

    @Query("DELETE FROM rutaentrega")
    suspend fun deleteAllRutas()
}

@Dao
interface UbicacionDao {
    @Query("SELECT * FROM ubicacionrepartidor ORDER BY createdAt DESC")
    fun getAllUbicaciones(): Flow<List<UbicacionEntity>>

    @Query("SELECT * FROM ubicacionrepartidor WHERE synced = 0 ORDER BY createdAt ASC")
    suspend fun getUbicacionesNoSincronizadas(): List<UbicacionEntity>

    @Query("SELECT * FROM ubicacionrepartidor WHERE idRepartidor = :idRepartidor ORDER BY createdAt DESC LIMIT 1")
    suspend fun getUltimaUbicacion(idRepartidor: Int): UbicacionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUbicacion(ubicacion: UbicacionEntity): Long

    @Update
    suspend fun updateUbicacion(ubicacion: UbicacionEntity)

    @Query("UPDATE ubicacionrepartidor SET synced = 1 WHERE id = :id")
    suspend fun marcarComoSincronizada(id: Int)

    @Query("DELETE FROM ubicacionrepartidor WHERE createdAt < :timestamp")
    suspend fun deleteUbicacionesAntiguas(timestamp: Long)

    @Query("DELETE FROM ubicacionrepartidor")
    suspend fun deleteAllUbicaciones()
}

@Dao
interface UsuarioDao{

    @Query("SELECT * FROM usuario WHERE rol = :rol")
    suspend fun getUsuariosPorRol(rol: String): List<UsuarioEntity>
}

@Dao
interface RutaPedidoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rutaPedido: RutaPedidoEntity)

    @Query("SELECT * FROM rutapedido WHERE idRuta = :idRuta")
    fun getPedidosDeRuta(idRuta: Int): Flow<List<RutaPedidoEntity>>
}
