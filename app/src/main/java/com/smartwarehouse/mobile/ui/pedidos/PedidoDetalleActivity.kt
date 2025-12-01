package com.smartwarehouse.mobile.ui.pedidos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.adapter.DetallePedidoAdapter
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.showToast
import com.smartwarehouse.mobile.utils.toDate
import com.smartwarehouse.mobile.utils.toFormattedString

class PedidoDetalleActivity : AppCompatActivity() {

    private val viewModel: PedidoDetalleViewModel by viewModels()

    private lateinit var tvIdPedido: TextView
    private lateinit var tvEstado: TextView
    private lateinit var tvFechaPedido: TextView
    private lateinit var tvFechaEntrega: TextView
    private lateinit var cardCliente: LinearLayout
    private lateinit var tvNombreCliente: TextView
    private lateinit var tvDireccionCliente: TextView
    private lateinit var tvTelefonoCliente: TextView
    private lateinit var btnLlamarCliente: ImageButton
    private lateinit var btnVerMapa: Button
    private lateinit var recyclerDetalles: RecyclerView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvIva: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnCambiarEstado: Button
    private lateinit var btnSiguienteEstado: Button
    private lateinit var progressBar: ProgressBar

    private val detalleAdapter = DetallePedidoAdapter()
    private var idPedido: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedido_detalle)

        idPedido = intent.getIntExtra("ID_PEDIDO", -1)
        if (idPedido == -1) {
            showToast("Error: ID de pedido inválido")
            finish()
            return
        }

        setupToolbar()
        initializeViews()
        setupRecyclerView()
        setupObservers()
        setupListeners()

        viewModel.cargarPedido(idPedido)
        viewModel.cargarDetalles(idPedido)
        viewModel.cargarTotales(idPedido)
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Detalle del Pedido"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun initializeViews() {
        tvIdPedido = findViewById(R.id.tvIdPedido)
        tvEstado = findViewById(R.id.tvEstado)
        tvFechaPedido = findViewById(R.id.tvFechaPedido)
        tvFechaEntrega = findViewById(R.id.tvFechaEntrega)

        // Verificar si existe cardCliente (para repartidores)
        try {
            cardCliente = findViewById(R.id.cardCliente)
            tvNombreCliente = findViewById(R.id.tvNombreCliente)
            tvDireccionCliente = findViewById(R.id.tvDireccionCliente)
            tvTelefonoCliente = findViewById(R.id.tvTelefonoCliente)
            btnLlamarCliente = findViewById(R.id.btnLlamarCliente)
            btnVerMapa = findViewById(R.id.btnVerMapa)
        } catch (e: Exception) {
            // Si no existen estas vistas, no pasa nada
            android.util.Log.w("PedidoDetalle", "Vistas de cliente no encontradas")
        }

        recyclerDetalles = findViewById(R.id.recyclerDetalles)
        tvSubtotal = findViewById(R.id.tvSubtotal)
        tvIva = findViewById(R.id.tvIva)
        tvTotal = findViewById(R.id.tvTotal)
        btnCambiarEstado = findViewById(R.id.btnCambiarEstado)
        btnSiguienteEstado = findViewById(R.id.btnSiguienteEstado)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        recyclerDetalles.apply {
            layoutManager = LinearLayoutManager(this@PedidoDetalleActivity)
            adapter = detalleAdapter
        }
    }

    private fun setupObservers() {
        // Observer del pedido
        viewModel.pedido.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.let { pedido ->
                        mostrarInformacionPedido(pedido)
                        configurarBotones(pedido)
                    }
                }
                is NetworkResult.Error -> {
                    showToast(result.message ?: "Error al cargar pedido")
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Observer de detalles
        viewModel.detalles.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    detalleAdapter.submitList(result.data ?: emptyList())
                }
                is NetworkResult.Error -> {
                    showToast("Error al cargar detalles")
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Observer de totales
        viewModel.totales.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    result.data?.let { totales ->
                        tvSubtotal.text = String.format("%.2f €", totales.subtotal)
                        tvIva.text = String.format("%.2f €", totales.iva)
                        tvTotal.text = String.format("%.2f €", totales.total)
                    }
                }
                is NetworkResult.Error -> {
                    tvSubtotal.text = "-- €"
                    tvIva.text = "-- €"
                    tvTotal.text = "-- €"
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Observer de cambio de estado
        viewModel.cambioEstadoResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    showToast("Estado actualizado correctamente")
                }
                is NetworkResult.Error -> {
                    showToast(result.message ?: "Error al cambiar estado")
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Observer de loading
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun mostrarInformacionPedido(pedido: com.smartwarehouse.mobile.data.model.response.Pedido) {
        tvIdPedido.text = "Pedido #${pedido.id}"
        tvEstado.text = pedido.getEstadoTexto()
        tvEstado.setTextColor(pedido.getEstadoColor())

        val fecha = pedido.fechaPedido.toDate()?.toFormattedString() ?: pedido.fechaPedido
        tvFechaPedido.text = "Realizado: $fecha"

        if (pedido.fechaEntrega != null) {
            val fechaEntrega = pedido.fechaEntrega.toDate()?.toFormattedString() ?: pedido.fechaEntrega
            tvFechaEntrega.text = "Entregado: $fechaEntrega"
            tvFechaEntrega.visibility = View.VISIBLE
        } else {
            tvFechaEntrega.visibility = View.GONE
        }

        // Información del cliente (solo visible para repartidores)
        try {
            if (viewModel.esRepartidor() && ::cardCliente.isInitialized) {
                cardCliente.visibility = View.VISIBLE
                tvNombreCliente.text = pedido.nombreCliente ?: "Cliente #${pedido.idCliente}"
                tvDireccionCliente.text = pedido.direccionEntrega ?: "Dirección no disponible"
                tvTelefonoCliente.text = pedido.telefonoCliente ?: "Teléfono no disponible"
            } else if (::cardCliente.isInitialized) {
                cardCliente.visibility = View.GONE
            }
        } catch (e: Exception) {
            android.util.Log.w("PedidoDetalle", "Error al mostrar info del cliente", e)
        }
    }

    private fun configurarBotones(pedido: com.smartwarehouse.mobile.data.model.response.Pedido) {
        // Solo repartidores pueden cambiar estados
        val puedeModificar = viewModel.esRepartidor() &&
                pedido.idRepartidor == viewModel.getUserId()

        if (puedeModificar && pedido.estado.name != "ENTREGADO") {
            btnSiguienteEstado.visibility = View.VISIBLE
            btnSiguienteEstado.text = pedido.getTextoBotonSiguienteEstado()

            btnCambiarEstado.visibility = View.VISIBLE
        } else {
            btnSiguienteEstado.visibility = View.GONE
            btnCambiarEstado.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        btnCambiarEstado.setOnClickListener {
            mostrarDialogoCambiarEstado()
        }

        btnSiguienteEstado.setOnClickListener {
            confirmarCambioEstado()
        }

        try {
            if (::btnLlamarCliente.isInitialized) {
                btnLlamarCliente.setOnClickListener {
                    llamarCliente()
                }
            }

            if (::btnVerMapa.isInitialized) {
                btnVerMapa.setOnClickListener {
                    abrirMapaUbicacion()
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("PedidoDetalle", "Error al configurar listeners del cliente", e)
        }
    }

    private fun confirmarCambioEstado() {
        val pedido = (viewModel.pedido.value as? NetworkResult.Success)?.data ?: return
        val siguienteEstado = pedido.getEstadoSiguiente() ?: return

        val mensaje = when (siguienteEstado) {
            com.smartwarehouse.mobile.data.model.response.EstadoPedido.ENTREGADO ->
                "¿Confirmar que el pedido ha sido entregado al cliente?"
            else ->
                "¿Cambiar el estado del pedido a ${siguienteEstado.name.lowercase().replace("_", " ")}?"
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar cambio de estado")
            .setMessage(mensaje)
            .setPositiveButton("Confirmar") { _, _ ->
                viewModel.avanzarAlSiguienteEstado(idPedido)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoCambiarEstado() {
        val estados = arrayOf("Pendiente", "Preparado", "En Reparto", "Entregado")

        AlertDialog.Builder(this)
            .setTitle("Cambiar Estado")
            .setItems(estados) { _, which ->
                val nuevoEstado = when (which) {
                    0 -> "pendiente"
                    1 -> "preparado"
                    2 -> "en_reparto"
                    3 -> "entregado"
                    else -> return@setItems
                }
                viewModel.cambiarEstado(idPedido, nuevoEstado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun llamarCliente() {
        try {
            if (::tvTelefonoCliente.isInitialized) {
                val telefono = tvTelefonoCliente.text.toString()
                if (telefono != "Teléfono no disponible" && telefono.isNotBlank()) {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$telefono")
                    }
                    startActivity(intent)
                } else {
                    showToast("Teléfono no disponible")
                }
            }
        } catch (e: Exception) {
            showToast("Error al intentar llamar")
            android.util.Log.e("PedidoDetalle", "Error al llamar", e)
        }
    }

    private fun abrirMapaUbicacion() {
        try {
            if (::tvDireccionCliente.isInitialized) {
                val direccion = tvDireccionCliente.text.toString()
                if (direccion != "Dirección no disponible" && direccion.isNotBlank()) {
                    val uri = Uri.parse("geo:0,0?q=$direccion")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.google.android.apps.maps")

                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        showToast("Google Maps no está instalado")
                    }
                } else {
                    showToast("Dirección no disponible")
                }
            }
        } catch (e: Exception) {
            showToast("Error al abrir el mapa")
            android.util.Log.e("PedidoDetalle", "Error al abrir mapa", e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_pedido_detalle, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.cargarPedido(idPedido)
                viewModel.cargarDetalles(idPedido)
                viewModel.cargarTotales(idPedido)
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}