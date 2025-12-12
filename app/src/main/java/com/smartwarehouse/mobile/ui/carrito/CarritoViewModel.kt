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

    private val _items = MutableLiveData<List<ItemCarrito>>()
    val items: LiveData<List<ItemCarrito>> = _items

    private val _subtotal = MutableLiveData<Double>()
    val subtotal: LiveData<Double> = _subtotal

    private val _iva = MutableLiveData<Double>()
    val iva: LiveData<Double> = _iva

    private val _total = MutableLiveData<Double>()
    val total: LiveData<Double> = _total

    private val _crearPedidoResult = MutableLiveData<NetworkResult<Boolean>>()
    val crearPedidoResult: LiveData<NetworkResult<Boolean>> = _crearPedidoResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        actualizarCarrito()
    }


    fun crearPedidoConGeocodificacion(
        direccion: String,
        ciudad: String,
        codigoPostal: String,
        notas: String?
    ) {
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

        _isLoading.value = true
        _crearPedidoResult.value = NetworkResult.Loading()

        viewModelScope.launch {
            try {

                val itemsParaActualizar = carrito.items.map { item ->
                    ItemCarrito(item.producto, item.cantidad)
                }.toList()

                Log.d("CarritoViewModel", "Items guardados para actualizar: ${itemsParaActualizar.size}")

                val (stockValido, mensajeStock) = productoRepository.verificarStockDisponible(itemsParaActualizar)
                if (!stockValido) {
                    _crearPedidoResult.value = NetworkResult.Error(mensajeStock)
                    _isLoading.value = false
                    return@launch
                }

                val (lat, lng) = calcularCoordenadasSuspend(direccion, ciudad, codigoPostal)

                val result = productoRepository.crearPedido(
                    direccion = direccion,
                    ciudad = ciudad,
                    codigoPostal = codigoPostal,
                    notas = notas,
                    latitud = lat,
                    longitud = lng
                )

                if (result is NetworkResult.Success) {
                    Log.d("CarritoViewModel", "✅ Pedido creado, actualizando stock...")

                    val stockActualizado = productoRepository.actualizarStockProductos(itemsParaActualizar)

                    if (stockActualizado) {
                        Log.d("CarritoViewModel", "✅ Stock actualizado correctamente")
                        vaciarCarrito()
                        _crearPedidoResult.value = result
                    } else {
                        Log.e("CarritoViewModel", "⚠️ Pedido creado pero hubo errores al actualizar stock")
                        _crearPedidoResult.value = NetworkResult.Error("Pedido creado pero error al actualizar stock")
                    }
                } else {

                    _crearPedidoResult.value = result
                }

            } catch (e: Exception) {
                Log.e("CarritoViewModel", "Error al crear pedido", e)
                _crearPedidoResult.value = NetworkResult.Error("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

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

    fun actualizarCarrito() {
        _items.value = carrito.items.map { item ->
            ItemCarrito(item.producto, item.cantidad)
        }
        _subtotal.value = carrito.getSubtotal()
        _iva.value = carrito.getIVA()
        _total.value = carrito.getTotal()
    }

    fun incrementarCantidad(idProducto: Int) {
        carrito.items.find { it.producto.idProducto == idProducto }?.let { item ->
            if (item.cantidad < item.producto.stock) {
                item.incrementar()
                actualizarCarrito()
                Log.d("CarritoVM", "Incrementado producto $idProducto. Nueva cantidad: ${item.cantidad}")
            }
        }
    }

    fun decrementarCantidad(idProducto: Int) {
        carrito.items.find { it.producto.idProducto == idProducto }?.let { item ->
            if (item.cantidad > 1) {
                item.decrementar()
                actualizarCarrito()
                Log.d("CarritoVM", "Decrementado producto $idProducto. Nueva cantidad: ${item.cantidad}")
            }
        }
    }

    fun eliminarItem(idProducto: Int) {
        carrito.eliminarProducto(idProducto)
        actualizarCarrito()
        Log.d("CarritoVM", "Eliminado producto $idProducto")
    }

    fun vaciarCarrito() {
        carrito.vaciar()
        actualizarCarrito()
    }

    fun carritoEstaVacio(): Boolean = carrito.isEmpty()
}