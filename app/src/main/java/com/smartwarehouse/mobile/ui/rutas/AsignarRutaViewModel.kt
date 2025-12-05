package com.smartwarehouse.mobile.ui.rutas

import android.app.Application
import androidx.lifecycle.*
import com.smartwarehouse.mobile.data.local.AppDatabase
import com.smartwarehouse.mobile.data.local.entity.PedidoEntity
import com.smartwarehouse.mobile.data.local.entity.RutaEntity
import com.smartwarehouse.mobile.data.local.entity.RutaPedidoEntity
import com.smartwarehouse.mobile.data.local.entity.UsuarioEntity
import com.smartwarehouse.mobile.data.local.mappers.toEntity
import com.smartwarehouse.mobile.data.repository.AuthRepository
import com.smartwarehouse.mobile.data.repository.RutaRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AsignarRutaViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val pedidoDao = database.pedidoDao()
    private val usuarioDao = database.usuarioDao()
    private val rutaDao = database.rutaDao()
    private val rutaPedidoDao =  database.rutaPedidoDao()

    // Pedidos pendientes
    val pedidosPendientes: LiveData<List<PedidoEntity>> = pedidoDao
        .getPedidosByEstado("pendiente")
        .asLiveData()

    // Repartidores
    val repartidores = MutableLiveData<List<UsuarioEntity>>()

    init {
        cargarRepartidores()
    }

    private fun cargarRepartidores() {
        viewModelScope.launch {
            val lista = usuarioDao.getUsuariosPorRol("repartidor")
            repartidores.postValue(lista)
        }
    }

    // Crear ruta y asignar pedidos seleccionados
    fun crearRuta(
        idRepartidor: Int,
        pedidosSeleccionados: List<PedidoEntity>,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // 1️⃣ Crear ruta
                val ruta = RutaEntity(
                    idRuta = (0..Int.MAX_VALUE).random(),   // Si quieres autogenerate, lo cambiamos
                    idRepartidor = idRepartidor,
                    fechaRuta = fechaHoy,
                    distanciaEstimadaKm = null,
                    duracionEstimadaMin = null,
                    estado = "pendiente"
                )

                rutaDao.insertRuta(ruta)

                // 2️⃣ Por cada pedido seleccionado: actualizar + insertar relación
                pedidosSeleccionados.forEach { pedido ->

                    // Actualizar pedido -> en reparto
                    val pedidoActualizado = pedido.copy(
                        idRepartidor = idRepartidor,
                        estado = "en_reparto"
                    )
                    pedidoDao.updatePedido(pedidoActualizado)

                    // 3️⃣ Insertar en tabla intermedia
                    val rp = RutaPedidoEntity(
                        idRuta = ruta.idRuta,
                        idPedido = pedido.idPedido
                    )
                    rutaPedidoDao.insert(rp)
                }

                onComplete(true, "Ruta creada y pedidos asignados correctamente")

            } catch (e: Exception) {
                onComplete(false, e.message ?: "Error desconocido")
            }
        }
    }

}
