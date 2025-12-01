package com.smartwarehouse.mobile.ui.rutas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.model.response.Ruta
import com.smartwarehouse.mobile.data.repository.RutaRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.launch

class RutasViewModel(application: Application) : AndroidViewModel(application) {

    private val rutaRepository = RutaRepository(application)

    private val _rutas = MutableLiveData<NetworkResult<List<Ruta>>>()
    val rutas: LiveData<NetworkResult<List<Ruta>>> = _rutas

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        cargarRutas()
    }

    fun cargarRutas() {
        _isLoading.value = true
        _rutas.value = NetworkResult.Loading()

        viewModelScope.launch {
            val result = rutaRepository.getRutasRepartidor()
            _rutas.value = result
            _isLoading.value = false
        }
    }

    fun filtrarRutasDeHoy() {
        // TODO: Implementar filtro por fecha actual
        cargarRutas()
    }

    fun getRutasPendientes(): List<Ruta> {
        return (_rutas.value as? NetworkResult.Success)?.data
            ?.filter { it.estado == com.smartwarehouse.mobile.data.model.response.EstadoRuta.PENDIENTE }
            ?: emptyList()
    }

    fun getRutasEnCurso(): List<Ruta> {
        return (_rutas.value as? NetworkResult.Success)?.data
            ?.filter { it.estado == com.smartwarehouse.mobile.data.model.response.EstadoRuta.EN_CURSO }
            ?: emptyList()
    }
}