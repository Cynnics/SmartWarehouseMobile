package com.smartwarehouse.mobile

import android.app.Application
import android.util.Log
import com.smartwarehouse.mobile.utils.GeocodingHelper
import com.smartwarehouse.mobile.service.SyncWorker

/**
 * Clase Application principal
 * Se ejecuta cuando la app arranca
 */
class SmartWarehouseApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializar WorkManager para sincronización periódica
        SyncWorker.Companion.schedule(this)

        // Limpiar cache de geocoding al iniciar
        GeocodingHelper.clearCache()

        // Log de inicio
        Log.d("SmartWarehouseApp", "App inicializada correctamente")
    }

    override fun onLowMemory() {
        super.onLowMemory()

        // Liberar cache si hay poca memoria
        GeocodingHelper.clearCache()
    }
}