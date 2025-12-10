package com.smartwarehouse.mobile.ui.rutas

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.smartwarehouse.mobile.data.model.response.*
import com.smartwarehouse.mobile.data.repository.RutaRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.launch

class RutaDetalleViewModel(application: Application) : AndroidViewModel(application) {

    private val rutaRepository = RutaRepository(application)

    private val _ruta = MutableLiveData<NetworkResult<Ruta>>()
    val ruta: LiveData<NetworkResult<Ruta>> = _ruta

    private val _pedidos = MutableLiveData<NetworkResult<List<Pedido>>>()
    val pedidos: LiveData<NetworkResult<List<Pedido>>> = _pedidos

    private val _ubicaciones = MutableLiveData<NetworkResult<List<UbicacionRepartidorResponse>>>()
    val ubicaciones: LiveData<NetworkResult<List<UbicacionRepartidorResponse>>> = _ubicaciones

    private val _cambioEstadoResult = MutableLiveData<NetworkResult<Boolean>>()
    val cambioEstadoResult: LiveData<NetworkResult<Boolean>> = _cambioEstadoResult

    private val _ubicacionEnviada = MutableLiveData<NetworkResult<Boolean>>()
    val ubicacionEnviada: LiveData<NetworkResult<Boolean>> = _ubicacionEnviada

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Para el mapa
    private val _waypoints = MutableLiveData<List<LatLng>>()
    val waypoints: LiveData<List<LatLng>> = _waypoints

    fun cargarRuta(idRuta: Int) {
        _isLoading.value = true

        viewModelScope.launch {
            val result = rutaRepository.getRutaById(idRuta)
            _ruta.value = result
            _isLoading.value = false
        }
    }

    fun cargarPedidosDeRuta(idRuta: Int) {
        viewModelScope.launch {
            val result = rutaRepository.getPedidosDeRuta(idRuta)
            _pedidos.value = result

            // Convertir pedidos a waypoints para el mapa
            if (result is NetworkResult.Success) {
                // TODO: Aquí necesitarías las coordenadas de cada pedido
                // Por ahora, usaremos coordenadas simuladas
                // En producción, deberías tener las direcciones con coordenadas en tu BD
            }
        }
    }

    fun cargarUbicacionesRuta(idRuta: Int) {
        viewModelScope.launch {
            val result = rutaRepository.getUbicacionesDeRuta(idRuta)
            _ubicaciones.value = result
        }
    }

    fun iniciarRuta(idRuta: Int) {
        _isLoading.value = true

        viewModelScope.launch {
            val result = rutaRepository.cambiarEstadoRuta(idRuta, "en_curso")
            _cambioEstadoResult.value = result

            if (result is NetworkResult.Success) {
                cargarRuta(idRuta)
            }

            _isLoading.value = false
        }
    }

    fun completarRuta(idRuta: Int) {
        _isLoading.value = true

        viewModelScope.launch {
            val result = rutaRepository.cambiarEstadoRuta(idRuta, "completada")
            _cambioEstadoResult.value = result

            if (result is NetworkResult.Success) {
                cargarRuta(idRuta)
            }

            _isLoading.value = false
        }
    }

    fun enviarUbicacionActual(latitud: Double, longitud: Double) {
        viewModelScope.launch {
            val result = rutaRepository.enviarUbicacion(latitud, longitud)
            _ubicacionEnviada.value = result
        }
    }

    fun guardarDistanciaYDuracion(idRuta: Int, distanciaKm: Double, duracionMin: Int) {
        viewModelScope.launch {
            val result = rutaRepository.actualizarDistanciaYDuracion(
                idRuta,
                distanciaKm,
                duracionMin
            )

            if (result is NetworkResult.Success) {
                Log.d("RutaDetalleVM", "Distancia y duración guardadas correctamente")
            }
        }
    }

    // Obtener coordenadas del centro de la ruta (para centrar el mapa)
    fun getCentroRuta(): LatLng {
        val pedidosList = (_pedidos.value as? NetworkResult.Success)?.data

        if (pedidosList.isNullOrEmpty()) {
            // Coordenadas por defecto (Madrid)
            return LatLng(40.4168, -3.7038)
        }

        // TODO: Calcular el centro promedio de todos los pedidos
        // Por ahora devolvemos Madrid
        return LatLng(40.4168, -3.7038)
    }
}