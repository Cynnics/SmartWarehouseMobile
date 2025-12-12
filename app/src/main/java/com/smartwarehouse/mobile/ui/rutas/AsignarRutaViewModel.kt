package com.smartwarehouse.mobile.ui.rutas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.model.response.Pedido
import com.smartwarehouse.mobile.data.model.response.UsuarioResponse
import com.smartwarehouse.mobile.data.repository.PedidoRepository
import com.smartwarehouse.mobile.data.repository.RutaRepository
import com.smartwarehouse.mobile.data.repository.UsuarioRepository
import com.smartwarehouse.mobile.utils.GeocodingHelper
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AsignarRutaViewModel(application: Application) : AndroidViewModel(application) {

    private val pedidoRepository = PedidoRepository(application)
    private val rutaRepository = RutaRepository(application)
    private val usuarioRepository = UsuarioRepository(application)

    private val _pedidosPendientes = MutableLiveData<NetworkResult<List<Pedido>>>()
    val pedidosPendientes: LiveData<NetworkResult<List<Pedido>>> = _pedidosPendientes

    private val _repartidores = MutableLiveData<NetworkResult<List<UsuarioResponse>>>()
    val repartidores: LiveData<NetworkResult<List<UsuarioResponse>>> = _repartidores

    private val _crearRutaResult = MutableLiveData<NetworkResult<Boolean>>()
    val crearRutaResult: LiveData<NetworkResult<Boolean>> = _crearRutaResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        cargarPedidosPendientes()
        cargarRepartidores()
    }

    fun cargarPedidosPendientes() {
        _isLoading.value = true
        _pedidosPendientes.value = NetworkResult.Loading()

        viewModelScope.launch {
            try {
                val result = pedidoRepository.getPedidos()

                when (result) {
                    is NetworkResult.Success -> {
                        val pedidosPendientes = result.data?.filter { pedido ->
                            pedido.estado == com.smartwarehouse.mobile.data.model.response.EstadoPedido.PENDIENTE
                        } ?: emptyList()

                        _pedidosPendientes.value = NetworkResult.Success(pedidosPendientes)
                    }
                    is NetworkResult.Error -> {
                        _pedidosPendientes.value = NetworkResult.Error(
                            result.message ?: "Error al cargar pedidos"
                        )
                    }
                    is NetworkResult.Loading -> {}
                }
            } catch (e: Exception) {
                _pedidosPendientes.value = NetworkResult.Error(
                    "Error: ${e.localizedMessage}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cargarRepartidores() {
        viewModelScope.launch {
            try {
                val result = usuarioRepository.getRepartidores()

                when (result) {
                    is NetworkResult.Success -> {
                        _repartidores.value = NetworkResult.Success(result.data ?: emptyList())
                    }
                    is NetworkResult.Error -> {
                        _repartidores.value = NetworkResult.Error(
                            result.message ?: "Error al cargar repartidores"
                        )
                    }
                    is NetworkResult.Loading -> {}
                }
            } catch (e: Exception) {
                _repartidores.value = NetworkResult.Error(
                    "Error: ${e.localizedMessage}"
                )
            }
        }
    }

    fun crearRuta(
        idRepartidor: Int,
        pedidosSeleccionados: List<Pedido>,
        fechaRuta: Date
    ) {
        if (pedidosSeleccionados.isEmpty()) {
            _crearRutaResult.value = NetworkResult.Error("Selecciona al menos un pedido")
            return
        }

        _isLoading.value = true
        _crearRutaResult.value = NetworkResult.Loading()

        viewModelScope.launch {
            try {
                val pedidosConCoordenadas = geocodificarPedidos(pedidosSeleccionados)

                if (pedidosConCoordenadas.isEmpty()) {
                    _crearRutaResult.value = NetworkResult.Error(
                        "No se pudieron geocodificar las direcciones de los pedidos"
                    )
                    _isLoading.value = false
                    return@launch
                }

                val distanciaEstimadaKm = calcularDistanciaTotal(pedidosConCoordenadas)
                val duracionEstimadaMin = calcularDuracionEstimada(pedidosConCoordenadas)

                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val fechaStr = formatter.format(fechaRuta)

                val resultCrearRuta = rutaRepository.crearRuta(
                    idRepartidor = idRepartidor,
                    fechaRuta = fechaStr,
                    distanciaEstimadaKm = distanciaEstimadaKm,
                    duracionEstimadaMin = duracionEstimadaMin
                )

                when (resultCrearRuta) {
                    is NetworkResult.Success -> {
                        val idRuta = resultCrearRuta.data

                        if (idRuta == null) {
                            _crearRutaResult.value = NetworkResult.Error(
                                "Error: No se recibió ID de la ruta creada"
                            )
                            _isLoading.value = false
                            return@launch
                        }

                        val resultAsignar = asignarPedidosARuta(
                            idRuta = idRuta,
                            pedidos = pedidosSeleccionados,
                            idRepartidor = idRepartidor
                        )

                        _crearRutaResult.value = resultAsignar
                    }
                    is NetworkResult.Error -> {
                        _crearRutaResult.value = NetworkResult.Error(
                            resultCrearRuta.message ?: "Error al crear la ruta"
                        )
                    }
                    is NetworkResult.Loading -> {}
                }
            } catch (e: Exception) {
                _crearRutaResult.value = NetworkResult.Error(
                    "Error inesperado: ${e.localizedMessage}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun geocodificarPedidos(pedidos: List<Pedido>): List<Pedido> {
        return pedidos.mapNotNull { pedido ->
            if (pedido.tieneCoordenadasValidas()) {

                pedido
            } else {
                val direccionCompleta = pedido.getDireccionCompleta()

                if (GeocodingHelper.isValidAddress(direccionCompleta)) {
                    val coordenadas = GeocodingHelper.getCoordinatesFromAddressWithCache(
                        GeocodingHelper.normalizeAddress(direccionCompleta)
                    )

                    if (coordenadas != null) {
                        pedido.copy(
                            latitud = coordenadas.latitude,
                            longitud = coordenadas.longitude
                        )
                    } else {
                        android.util.Log.w("AsignarRuta",
                            "No se pudo geocodificar pedido #${pedido.id}")
                        null
                    }
                } else {
                    android.util.Log.w("AsignarRuta",
                        "Dirección inválida para pedido #${pedido.id}")
                    null
                }
            }
        }
    }

    private suspend fun asignarPedidosARuta(
        idRuta: Int,
        pedidos: List<Pedido>,
        idRepartidor: Int
    ): NetworkResult<Boolean> {
        return try {
            val resultados = pedidos.map { pedido ->
                viewModelScope.async {
                    val resultAsignacion = rutaRepository.asignarPedidoARuta(idRuta, pedido.id)

                    if (resultAsignacion is NetworkResult.Success) {
                        pedidoRepository.cambiarEstadoPedido(pedido.id, "preparado")
                    } else {
                        resultAsignacion
                    }
                }
            }.awaitAll()

            val todosExitosos = resultados.all { it is NetworkResult.Success }

            if (todosExitosos) {
                NetworkResult.Success(true)
            } else {
                val errores = resultados.filterIsInstance<NetworkResult.Error<*>>()
                NetworkResult.Error(
                    "Algunos pedidos no pudieron asignarse: ${errores.size} errores"
                )
            }
        } catch (e: Exception) {
            NetworkResult.Error("Error al asignar pedidos: ${e.localizedMessage}")
        }
    }

    private fun calcularDistanciaTotal(pedidos: List<Pedido>): Double {
        return 5.0 + (pedidos.size * 2.0)
    }


    private fun calcularDuracionEstimada(pedidos: List<Pedido>): Int {
        return 30 + (pedidos.size * 15)
    }
}

private fun Pedido.copy(
    latitud: Double? = this.latitud,
    longitud: Double? = this.longitud
): Pedido {
    return Pedido(
        id = this.id,
        idCliente = this.idCliente,
        idRepartidor = this.idRepartidor,
        estado = this.estado,
        fechaPedido = this.fechaPedido,
        fechaEntrega = this.fechaEntrega,
        direccionEntrega = this.direccionEntrega,
        ciudad = this.ciudad,
        codigoPostal = this.codigoPostal,
        latitud = latitud,
        longitud = longitud,
        nombreCliente = this.nombreCliente,
        telefonoCliente = this.telefonoCliente
    )
}