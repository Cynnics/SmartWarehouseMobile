package com.smartwarehouse.mobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartwarehouse.mobile.data.local.converters.DateConverter
import com.smartwarehouse.mobile.data.local.dao.*
import com.smartwarehouse.mobile.data.local.entity.*

@Database(
    entities = [
        ProductoEntity::class,
        PedidoEntity::class,
        DetallePedidoEntity::class,
        RutaEntity::class,
        UbicacionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao
    abstract fun pedidoDao(): PedidoDao
    abstract fun detallePedidoDao(): DetallePedidoDao
    abstract fun rutaDao(): RutaDao
    abstract fun ubicacionDao(): UbicacionDao

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
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}