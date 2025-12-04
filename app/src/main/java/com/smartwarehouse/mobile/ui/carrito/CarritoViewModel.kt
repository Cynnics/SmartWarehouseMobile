package com.smartwarehouse.mobile.ui.carrito

import android.app.Application
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.smartwarehouse.mobile.data.model.ItemCarrito
import com.smartwarehouse.mobile.data.repository.ProductoRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class CarritoViewModel(application: Application) : AndroidViewModel(application) {

    private val productoRepository = ProductoRepository(application)
    val carrito = ProductoRepository.carrito

    // ------- CAMPOS UI ------
    private val _direccion = MutableLiveData<String>()
    val direccion: LiveData<String> = _direccion

    private val _ciudad = MutableLiveData<String>()
    val ciudad: LiveData<String> = _ciudad

    private val _codigoPostal = MutableLiveData<String>()
    val codigoPostal: LiveData<String> = _codigoPostal

    private val _notas = MutableLiveData<String?>()
    val notas: LiveData<String?> = _notas

    // ------- LAT / LNG -------
    private val _latitud = MutableLiveData<Double?>()
    val latitud: LiveData<Double?> = _latitud

    private val _longitud = MutableLiveData<Double?>()
    val longitud: LiveData<Double?> = _longitud

    // ------- CARRITO -------
    private val _items = MutableLiveData<List<ItemCarrito>>()
    val items: LiveData<List<ItemCarrito>> = _items

    private val _subtotal = MutableLiveData<Double>()
    val subtotal: LiveData<Double> = _subtotal

    private val _iva = MutableLiveData<Double>()
    val iva: LiveData<Double> = _iva

    private val _total = MutableLiveData<Double>()
    val total: LiveData<Double> = _total

    // ------- RESULTADOS -------
    private val _crearPedidoResult = MutableLiveData<NetworkResult<Boolean>>()
    val crearPedidoResult: LiveData<NetworkResult<Boolean>> = _crearPedidoResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        actualizarCarrito()
    }

    // ================================================================
    // CARGAR DATOS UI DESDE ACTIVITY
    // ================================================================
    fun setDireccion(value: String) { _direccion.value = value }
    fun setCiudad(value: String) { _ciudad.value = value }
    fun setCodigoPostal(value: String) { _codigoPostal.value = value }
    fun setNotas(value: String?) { _notas.value = value }
    fun setLatitud (value: Double) {_latitud.value =value}
    fun setLongitud (value:Double) {_longitud.value = value}

    // ================================================================
    // CALCULAR LAT/LNG AUTOMÁTICAMENTE
    // ================================================================
    fun calcularCoordenadas() {
        viewModelScope.launch(Dispatchers.IO) {
            val direccionCompleta = "${direccion.value.orEmpty()}, ${ciudad.value.orEmpty()}, ${codigoPostal.value.orEmpty()}"
            try {
                Log.d("Geocoder", "Intentando obtener coordenadas para: $direccionCompleta")
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                val resultados = geocoder.getFromLocationName(direccionCompleta, 1)

                if (!resultados.isNullOrEmpty()) {
                    val loc = resultados[0]
                    Log.d("Geocoder", "Coordenadas encontradas: lat=${loc.latitude}, lng=${loc.longitude}")
                    _latitud.postValue(loc.latitude)
                    _longitud.postValue(loc.longitude)
                } else {
                    Log.d("Geocoder", "No se encontraron resultados para la dirección")
                    _latitud.postValue(0.0)
                    _longitud.postValue(0.0)
                }
            } catch (e: Exception) {
                Log.e("Geocoder", "Error al obtener coordenadas: ${e.message}", e)
                _latitud.postValue(0.0)
                _longitud.postValue(0.0)
            }
        }
    }

    suspend fun calcularCoordenadasSuspend(
        direccion: String,
        ciudad: String,
        codigoPostal: String
    ): Pair<Double, Double> = withContext(Dispatchers.IO) {
        val direccionCompleta = "$direccion, $ciudad, $codigoPostal"
        try {
            val geocoder = Geocoder(getApplication(), Locale.getDefault())
            val resultados = geocoder.getFromLocationName(direccionCompleta, 1)
            if (!resultados.isNullOrEmpty()) {
                val loc = resultados[0]
                Pair(loc.latitude, loc.longitude)
            } else {
                Pair(0.0, 0.0)
            }
        } catch (e: Exception) {
            Pair(0.0, 0.0)
        }
    }


    // ================================================================
    // MÉTODOS CARRITO
    // ================================================================
    fun actualizarCarrito() {
        _items.value = carrito.items.toList()
        _subtotal.value = carrito.getSubtotal()
        _iva.value = carrito.getIVA()
        _total.value = carrito.getTotal()
    }

    fun incrementarCantidad(idProducto: Int) {
        carrito.items.find { it.producto.idProducto == idProducto }?.incrementar()
        actualizarCarrito()
    }

    fun decrementarCantidad(idProducto: Int) {
        carrito.items.find { it.producto.idProducto == idProducto }?.decrementar()
        actualizarCarrito()
    }

    fun eliminarItem(idProducto: Int) {
        carrito.eliminarProducto(idProducto)
        actualizarCarrito()
    }

    fun vaciarCarrito() {
        carrito.vaciar()
        actualizarCarrito()
    }


    // ================================================================
    // CREAR PEDIDO
    // ================================================================
    fun crearPedido() {
        val direccionValue = _direccion.value
        val ciudadValue = _ciudad.value
        val cpValue = _codigoPostal.value
        val notasValue = _notas.value
        val lat = _latitud.value ?: 0.0
        val lng = _longitud.value ?: 0.0

        // ----- VALIDACIONES -----
        if (direccionValue.isNullOrBlank()) {
            _crearPedidoResult.value = NetworkResult.Error("La dirección es obligatoria")
            return
        }

        if (ciudadValue.isNullOrBlank()) {
            _crearPedidoResult.value = NetworkResult.Error("La ciudad es obligatoria")
            return
        }

        if (cpValue.isNullOrBlank()) {
            _crearPedidoResult.value = NetworkResult.Error("El código postal es obligatorio")
            return
        }

        if (carrito.isEmpty()) {
            _crearPedidoResult.value = NetworkResult.Error("El carrito está vacío")
            return
        }

        _isLoading.value = true
        _crearPedidoResult.value = NetworkResult.Loading()

        viewModelScope.launch {
            val result = productoRepository.crearPedido(
                direccion = direccionValue,
                ciudad = ciudadValue,
                codigoPostal = cpValue,
                notas = notasValue,
                latitud = lat,
                longitud = lng
            )

            _crearPedidoResult.value = result
            _isLoading.value = false

            if (result is NetworkResult.Success) {
                vaciarCarrito()
            }
        }
    }

    fun carritoEstaVacio(): Boolean = carrito.isEmpty()
}