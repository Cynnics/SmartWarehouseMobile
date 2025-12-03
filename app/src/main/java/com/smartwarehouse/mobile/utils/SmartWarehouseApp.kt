package com.smartwarehouse.mobile.utils

import android.app.Application
import com.smartwarehouse.mobile.data.sync.SyncWorker

/**
 * Clase Application principal
 * Se ejecuta cuando la app arranca
 */
class SmartWarehouseApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializar WorkManager para sincronización periódica
        SyncWorker.schedule(this)

        // Limpiar cache de geocoding al iniciar
        GeocodingHelper.clearCache()

        // Log de inicio
        android.util.Log.d("SmartWarehouseApp", "App inicializada correctamente")
    }

    override fun onLowMemory() {
        super.onLowMemory()

        // Liberar cache si hay poca memoria
        GeocodingHelper.clearCache()
    }
}