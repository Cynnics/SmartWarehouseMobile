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

/**
 * Worker para sincronización periódica de datos
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Sincronizar productos
            val productoRepo = ProductoRepository(applicationContext)
            productoRepo.syncProductos()

            // Sincronizar pedidos
            // val pedidoRepo = PedidoRepository(applicationContext)
            // pedidoRepo.syncPedidos()

            // Sincronizar rutas
            // val rutaRepo = RutaRepository(applicationContext)
            // rutaRepo.syncRutas()

            // Sincronizar ubicaciones pendientes
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

        // Obtener ubicaciones no sincronizadas
        val ubicaciones = ubicacionDao.getUbicacionesNoSincronizadas()

        ubicaciones.forEach { ubicacion ->
            try {
                // Enviar a la API
                rutaRepo.enviarUbicacion(ubicacion.latitud, ubicacion.longitud)

                // Marcar como sincronizada
                ubicacionDao.marcarComoSincronizada(ubicacion.id)
            } catch (e: Exception) {
                // Log error pero continuar con siguientes
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