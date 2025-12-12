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

        SyncWorker.schedule(this)

        GeocodingHelper.clearCache()

        Log.d("SmartWarehouseApp", "App inicializada correctamente")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        GeocodingHelper.clearCache()
    }
}