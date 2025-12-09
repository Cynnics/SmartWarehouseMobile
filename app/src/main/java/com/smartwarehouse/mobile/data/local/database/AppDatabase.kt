package com.smartwarehouse.mobile.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartwarehouse.mobile.data.local.converters.DateConverter
import com.smartwarehouse.mobile.data.local.dao.DetallePedidoDao
import com.smartwarehouse.mobile.data.local.dao.PedidoDao
import com.smartwarehouse.mobile.data.local.dao.ProductoDao
import com.smartwarehouse.mobile.data.local.dao.RutaDao
import com.smartwarehouse.mobile.data.local.dao.RutaPedidoDao
import com.smartwarehouse.mobile.data.local.dao.UbicacionDao
import com.smartwarehouse.mobile.data.local.dao.UsuarioDao
import com.smartwarehouse.mobile.data.local.entity.DetallePedidoEntity
import com.smartwarehouse.mobile.data.local.entity.PedidoEntity
import com.smartwarehouse.mobile.data.local.entity.ProductoEntity
import com.smartwarehouse.mobile.data.local.entity.RutaEntity
import com.smartwarehouse.mobile.data.local.entity.RutaPedidoEntity
import com.smartwarehouse.mobile.data.local.entity.UbicacionEntity
import com.smartwarehouse.mobile.data.local.entity.UsuarioEntity

@Database(
    entities = [
        ProductoEntity::class,
        PedidoEntity::class,
        DetallePedidoEntity::class,
        RutaEntity::class,
        UbicacionEntity::class,
        RutaPedidoEntity::class,
        UsuarioEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao
    abstract fun pedidoDao(): PedidoDao
    abstract fun detallePedidoDao(): DetallePedidoDao
    abstract fun rutaDao(): RutaDao
    abstract fun ubicacionDao(): UbicacionDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun rutaPedidoDao ()  : RutaPedidoDao
    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null


        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "smartwarehouse_db"
            )
                .fallbackToDestructiveMigration() // elimina la base de datos vieja
                .build()
        }


    }
}