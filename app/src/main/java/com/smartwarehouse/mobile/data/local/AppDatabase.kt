package com.smartwarehouse.mobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE ubicacionrepartidor ADD COLUMN fechaHora TEXT"
                )
            }
        }

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