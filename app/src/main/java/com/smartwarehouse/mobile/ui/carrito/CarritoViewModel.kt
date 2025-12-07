package com.smartwarehouse.mobile.ui.carrito

import android.app.Application
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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
    // ✅ MÉTODO PRINCIPAL: TODO EN UNO
    // ================================================================
    fun crearPedidoConGeocodificacion(
        direccion: String,
        ciudad: String,
        codigoPostal: String,
        notas: String?
    ) {
        // 1️⃣ Validaciones
        if (direccion.isBlank()) {
            _crearPedidoResult.value = NetworkResult.Error("La dirección es obligatoria")
            return
        }

        if (ciudad.isBlank()) {
            _crearPedidoResult.value = NetworkResult.Error("La ciudad es obligatoria")
            return
        }

        if (codigoPostal.isBlank()) {
            _crearPedidoResult.value = NetworkResult.Error("El código postal es obligatorio")
            return
        }

        if (carrito.isEmpty()) {
            _crearPedidoResult.value = NetworkResult.Error("El carrito está vacío")
            return
        }

        // 2️⃣ Proceso asíncrono
        _isLoading.value = true
        _crearPedidoResult.value = NetworkResult.Loading()

        viewModelScope.launch {
            try {
                // 3️⃣ Geocodificar dirección
                val (lat, lng) = calcularCoordenadasSuspend(direccion, ciudad, codigoPostal)

                // 4️⃣ Crear pedido en la API
                val result = productoRepository.crearPedido(
                    direccion = direccion,
                    ciudad = ciudad,
                    codigoPostal = codigoPostal,
                    notas = notas,
                    latitud = lat,
                    longitud = lng
                )

                // 5️⃣ Notificar resultado
                _crearPedidoResult.value = result

                // 6️⃣ Si éxito, vaciar carrito
                if (result is NetworkResult.Success) {
                    vaciarCarrito()
                }

            } catch (e: Exception) {
                Log.e("CarritoViewModel", "Error al crear pedido", e)
                _crearPedidoResult.value = NetworkResult.Error("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ================================================================
    // GEOCODIFICACIÓN (PRIVADO)
    // ================================================================
    private suspend fun calcularCoordenadasSuspend(
        direccion: String,
        ciudad: String,
        codigoPostal: String
    ): Pair<Double, Double> = withContext(Dispatchers.IO) {
        val direccionCompleta = "$direccion, $ciudad, $codigoPostal"
        try {
            Log.d("Geocoder", "Intentando obtener coordenadas para: $direccionCompleta")
            val geocoder = Geocoder(getApplication(), Locale.getDefault())
            val resultados = geocoder.getFromLocationName(direccionCompleta, 1)

            if (!resultados.isNullOrEmpty()) {
                val loc = resultados[0]
                Log.d("Geocoder", "Coordenadas encontradas: lat=${loc.latitude}, lng=${loc.longitude}")
                Pair(loc.latitude, loc.longitude)
            } else {
                Log.d("Geocoder", "No se encontraron resultados para la dirección")
                Pair(0.0, 0.0)
            }
        } catch (e: Exception) {
            Log.e("Geocoder", "Error al obtener coordenadas: ${e.message}", e)
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

    fun carritoEstaVacio(): Boolean = carrito.isEmpty()
}