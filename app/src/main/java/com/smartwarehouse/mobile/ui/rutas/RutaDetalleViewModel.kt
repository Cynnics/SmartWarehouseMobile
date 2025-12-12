package com.smartwarehouse.mobile.ui.rutas

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.smartwarehouse.mobile.data.model.response.*
import com.smartwarehouse.mobile.data.repository.PedidoRepository
import com.smartwarehouse.mobile.data.repository.RutaRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RutaDetalleViewModel(application: Application) : AndroidViewModel(application) {

    private val rutaRepository = RutaRepository(application)
    private val pedidoRepository = PedidoRepository(application)
    private val _ruta = MutableLiveData<NetworkResult<Ruta>>()
    val ruta: LiveData<NetworkResult<Ruta>> = _ruta
    private val _pedidos = MutableLiveData<NetworkResult<List<Pedido>>>()
    val pedidos: LiveData<NetworkResult<List<Pedido>>> = _pedidos
    private val _cambioEstadoResult = MutableLiveData<NetworkResult<Boolean>>()
    val cambioEstadoResult: LiveData<NetworkResult<Boolean>> = _cambioEstadoResult
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _ubicacionEnviada = MutableLiveData<NetworkResult<Boolean>>()
    val ubicacionEnviada: LiveData<NetworkResult<Boolean>> = _ubicacionEnviada
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

        }
    }
    fun iniciarRuta(idRuta: Int) {
        _isLoading.value = true

        viewModelScope.launch {
            val resultRuta = rutaRepository.cambiarEstadoRuta(idRuta, "en_curso")

            if (resultRuta is NetworkResult.Success) {
                val resultCambioEstados = cambiarEstadosPedidosDeRuta(idRuta, "en_reparto")

                if (resultCambioEstados is NetworkResult.Success) {
                    _cambioEstadoResult.value = NetworkResult.Success(true)
                    cargarRuta(idRuta)
                    cargarPedidosDeRuta(idRuta)
                } else {
                    _cambioEstadoResult.value = NetworkResult.Error(
                        "Ruta iniciada pero error al actualizar pedidos"
                    )
                }
            } else {
                _cambioEstadoResult.value = resultRuta
            }

            _isLoading.value = false
        }
    }

    fun completarRuta(idRuta: Int) {
        _isLoading.value = true

        viewModelScope.launch {
            val resultPedidos = cambiarEstadosPedidosDeRuta(idRuta, "entregado")

            if (resultPedidos is NetworkResult.Success) {
                val resultRuta = rutaRepository.cambiarEstadoRuta(idRuta, "completada")

                if (resultRuta is NetworkResult.Success) {
                    _cambioEstadoResult.value = NetworkResult.Success(true)
                    cargarRuta(idRuta)
                    cargarPedidosDeRuta(idRuta)
                } else {
                    _cambioEstadoResult.value = NetworkResult.Error(
                        "Pedidos actualizados pero error al completar ruta"
                    )
                }
            } else {
                _cambioEstadoResult.value = resultPedidos
            }

            _isLoading.value = false
        }
    }

    private suspend fun cambiarEstadosPedidosDeRuta(
        idRuta: Int,
        nuevoEstado: String
    ): NetworkResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val pedidosResult = rutaRepository.getPedidosDeRuta(idRuta)

                if (pedidosResult is NetworkResult.Success) {
                    val pedidos = pedidosResult.data ?: emptyList()

                    if (pedidos.isEmpty()) {
                        return@withContext NetworkResult.Error("No hay pedidos en esta ruta")
                    }

                    val resultados = pedidos.map { pedido ->
                        async {
                            pedidoRepository.cambiarEstadoPedido(pedido.id, nuevoEstado)
                        }
                    }.awaitAll()

                    val todosExitosos = resultados.all { it is NetworkResult.Success }

                    if (todosExitosos) {
                        Log.d("RutaDetalleVM", "✅ Todos los pedidos actualizados a $nuevoEstado")
                        NetworkResult.Success(true)
                    } else {
                        val errores = resultados.filterIsInstance<NetworkResult.Error<*>>()
                        Log.e("RutaDetalleVM", "❌ ${errores.size} pedidos fallaron al actualizar")
                        NetworkResult.Error("Error al actualizar ${errores.size} pedidos")
                    }
                } else {
                    NetworkResult.Error("Error al obtener pedidos de la ruta")
                }
            } catch (e: Exception) {
                Log.e("RutaDetalleVM", "Error al cambiar estados", e)
                NetworkResult.Error("Error: ${e.localizedMessage}")
            }
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
}