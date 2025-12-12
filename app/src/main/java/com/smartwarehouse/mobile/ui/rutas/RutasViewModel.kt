package com.smartwarehouse.mobile.ui.rutas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.local.database.AppDatabase
import com.smartwarehouse.mobile.data.local.mappers.toDomain
import com.smartwarehouse.mobile.data.local.mappers.toEntity
import com.smartwarehouse.mobile.data.repository.AuthRepository
import com.smartwarehouse.mobile.data.repository.RutaRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RutasViewModel(application: Application) : AndroidViewModel(application) {

    private val rutaRepository = RutaRepository(application)
    private val authRepository = AuthRepository(application)
    private val database = AppDatabase.getInstance(application)
    private val rutaDao = database.rutaDao()

    val rutas = rutaDao.getRutasByRepartidor(authRepository.getUserId())
        .map { entities -> entities.map { it.toDomain() } }
        .asLiveData(viewModelScope.coroutineContext)

    init {
        sincronizarRutas()
    }

    fun sincronizarRutas() {
        viewModelScope.launch {
            val result = rutaRepository.getRutasRepartidor()

            if (result is NetworkResult.Success) {
                val entities = result.data?.map { it.toEntity() } ?: emptyList()
                entities.forEach { rutaDao.insertRuta(it) }
            }
        }
    }



}