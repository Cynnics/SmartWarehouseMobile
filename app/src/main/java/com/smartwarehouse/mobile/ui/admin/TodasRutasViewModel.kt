package com.smartwarehouse.mobile.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.local.database.AppDatabase
import com.smartwarehouse.mobile.data.local.mappers.toDomain
import com.smartwarehouse.mobile.data.local.mappers.toEntity
import com.smartwarehouse.mobile.data.repository.RutaRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TodasRutasViewModel(application: Application) : AndroidViewModel(application) {

    private val rutaRepository = RutaRepository(application)
    private val database = AppDatabase.getInstance(application)
    private val rutaDao = database.rutaDao()

    // 🔥 Obtener TODAS las rutas del sistema (no filtradas por repartidor)
    val rutas = rutaDao.getAllRutas()
        .map { entities -> entities.map { it.toDomain() } }
        .asLiveData(viewModelScope.coroutineContext)

    init {
        sincronizarTodasRutas()
    }

    fun sincronizarTodasRutas() {
        viewModelScope.launch {
            // Obtener todas las rutas de la API (no filtradas)
            val result = rutaRepository.getRutas()

            if (result is NetworkResult.Success) {
                val entities = result.data?.map { it.toEntity() } ?: emptyList()
                entities.forEach { rutaDao.insertRuta(it) }

                android.util.Log.d("TodasRutasVM", "Sincronizadas ${entities.size} rutas totales")
            }
        }
    }
}