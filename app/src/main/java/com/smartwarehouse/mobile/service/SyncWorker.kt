package com.smartwarehouse.mobile.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smartwarehouse.mobile.data.local.database.AppDatabase
import com.smartwarehouse.mobile.data.repository.ProductoRepository
import com.smartwarehouse.mobile.data.repository.RutaRepository
import java.util.concurrent.TimeUnit


class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val productoRepo = ProductoRepository(applicationContext)
            productoRepo.syncProductos()

            syncUbicacionesPendientes()

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun syncUbicacionesPendientes() {
        val database = AppDatabase.Companion.getInstance(applicationContext)
        val ubicacionDao = database.ubicacionDao()
        val rutaRepo = RutaRepository(applicationContext)

        val ubicaciones = ubicacionDao.getUbicacionesNoSincronizadas()

        ubicaciones.forEach { ubicacion ->
            try {
                rutaRepo.enviarUbicacion(ubicacion.latitud, ubicacion.longitud)

                ubicacionDao.marcarComoSincronizada(ubicacion.id)
            } catch (e: Exception) {

                Log.e("SyncWorker", "Error sync ubicacion", e)
            }
        }
    }

    companion object {
        const val WORK_NAME = "sync_work"

        /**
         * Programa sincronización periódica cada 15 minutos
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}