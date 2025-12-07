package com.smartwarehouse.mobile.ui.rutas

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.adapter.PedidoSeleccionableAdapter
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.data.model.response.Pedido
import com.smartwarehouse.mobile.utils.showToast
import java.text.SimpleDateFormat
import java.util.*

class AsignarRutaActivity : AppCompatActivity() {

    private lateinit var recyclerPedidos: RecyclerView
    private lateinit var spinnerRepartidores: Spinner
    private lateinit var btnCrearRuta: MaterialButton
    private lateinit var btnSeleccionarFecha: MaterialButton
    private lateinit var tvFechaSeleccionada: TextView
    private lateinit var emptyView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvPedidosSeleccionados: TextView

    private val viewModel: AsignarRutaViewModel by viewModels()

    // Pedidos seleccionados
    private val selectedPedidos = mutableSetOf<Pedido>()

    // Fecha seleccionada (por defecto hoy)
    private var fechaSeleccionada: Date = Date()

    private val pedidoAdapter = PedidoSeleccionableAdapter { pedido, isChecked ->
        if (isChecked) {
            selectedPedidos.add(pedido)
        } else {
            selectedPedidos.remove(pedido)
        }
        actualizarContadorSeleccionados()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asignar_rutas)

        setupToolbar()
        initializeViews()
        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Mostrar fecha inicial
        actualizarFechaTexto()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Asignar Ruta"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun initializeViews() {
        recyclerPedidos = findViewById(R.id.recyclerPedidos)
        spinnerRepartidores = findViewById(R.id.spinnerRepartidores)
        btnCrearRuta = findViewById(R.id.btnCrearRuta)
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha)
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionada)
        emptyView = findViewById(R.id.emptyView)
        progressBar = findViewById(R.id.progressBar)
        tvPedidosSeleccionados = findViewById(R.id.tvPedidosSeleccionados)
    }

    private fun setupRecyclerView() {
        recyclerPedidos.apply {
            layoutManager = LinearLayoutManager(this@AsignarRutaActivity)
            adapter = pedidoAdapter
        }
    }

    private fun setupObservers() {
        // Observer de pedidos pendientes
        viewModel.pedidosPendientes.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    val pedidos = result.data ?: emptyList()
                    pedidoAdapter.submitList(pedidos)

                    if (pedidos.isEmpty()) {
                        emptyView.visibility = View.VISIBLE
                        recyclerPedidos.visibility = View.GONE
                        emptyView.text = "No hay pedidos pendientes"
                    } else {
                        emptyView.visibility = View.GONE
                        recyclerPedidos.visibility = View.VISIBLE
                    }
                }
                is NetworkResult.Error -> {
                    showToast(result.message ?: "Error al cargar pedidos")
                    emptyView.visibility = View.VISIBLE
                    recyclerPedidos.visibility = View.GONE
                    emptyView.text = "Error al cargar pedidos"
                }
                is NetworkResult.Loading -> {
                    progressBar.visibility = View.VISIBLE
                }
            }
        }

        // Observer de repartidores
        viewModel.repartidores.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    val repartidores = result.data ?: emptyList()

                    if (repartidores.isEmpty()) {
                        showToast("No hay repartidores disponibles")
                        btnCrearRuta.isEnabled = false
                        return@observe
                    }

                    val nombres = repartidores.map { "${it.nombre} (ID: ${it.idUsuario})" }
                    val adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_spinner_item,
                        nombres
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerRepartidores.adapter = adapter
                }
                is NetworkResult.Error -> {
                    showToast("Error al cargar repartidores")
                    btnCrearRuta.isEnabled = false
                }
                is NetworkResult.Loading -> {}
            }
        }

        // Observer del resultado de crear ruta
        viewModel.crearRutaResult.observe(this) { result ->
            progressBar.visibility = View.GONE

            when (result) {
                is NetworkResult.Success -> {
                    mostrarDialogoExito()
                }
                is NetworkResult.Error -> {
                    showToast(result.message ?: "Error al crear la ruta")
                }
                is NetworkResult.Loading -> {
                    progressBar.visibility = View.VISIBLE
                }
            }
        }

        // Observer de loading
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnCrearRuta.isEnabled = !isLoading
        }
    }

    private fun setupListeners() {
        btnSeleccionarFecha.setOnClickListener {
            mostrarSelectorFecha()
        }

        btnCrearRuta.setOnClickListener {
            validarYCrearRuta()
        }
    }

    private fun mostrarSelectorFecha() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha de la ruta")
            .setSelection(fechaSeleccionada.time)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            fechaSeleccionada = Date(selection)
            actualizarFechaTexto()
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun actualizarFechaTexto() {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        tvFechaSeleccionada.text = "Fecha: ${formatter.format(fechaSeleccionada)}"
    }

    private fun actualizarContadorSeleccionados() {
        tvPedidosSeleccionados.text = "${selectedPedidos.size} pedidos seleccionados"
        btnCrearRuta.isEnabled = selectedPedidos.isNotEmpty()
    }

    private fun validarYCrearRuta() {
        // Validar pedidos seleccionados
        if (selectedPedidos.isEmpty()) {
            showToast("Selecciona al menos un pedido")
            return
        }

        // Validar repartidor seleccionado
        val posicion = spinnerRepartidores.selectedItemPosition
        if (posicion == AdapterView.INVALID_POSITION) {
            showToast("Selecciona un repartidor")
            return
        }

        val repartidoresResult = viewModel.repartidores.value
        if (repartidoresResult !is NetworkResult.Success) {
            showToast("Error al obtener datos del repartidor")
            return
        }

        val repartidor = repartidoresResult.data?.getOrNull(posicion)
        if (repartidor == null) {
            showToast("Error: Repartidor no válido")
            return
        }

        // Mostrar diálogo de confirmación
        mostrarDialogoConfirmacion(repartidor.idUsuario, repartidor.nombre)
    }

    private fun mostrarDialogoConfirmacion(idRepartidor: Int, nombreRepartidor: String) {
        val mensaje = """
            ¿Crear ruta con los siguientes datos?
            
            Repartidor: $nombreRepartidor
            Fecha: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(fechaSeleccionada)}
            Pedidos: ${selectedPedidos.size}
            
            Los pedidos pasarán a estado "En Reparto"
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Confirmar Creación de Ruta")
            .setMessage(mensaje)
            .setPositiveButton("Crear Ruta") { _, _ ->
                crearRuta(idRepartidor)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun crearRuta(idRepartidor: Int) {
        viewModel.crearRuta(
            idRepartidor = idRepartidor,
            pedidosSeleccionados = selectedPedidos.toList(),
            fechaRuta = fechaSeleccionada
        )
    }

    private fun mostrarDialogoExito() {
        AlertDialog.Builder(this)
            .setTitle("✅ Ruta Creada")
            .setMessage("La ruta ha sido creada exitosamente y los pedidos han sido asignados al repartidor.")
            .setPositiveButton("Ver Rutas") { _, _ ->
                // TODO: Navegar a lista de rutas
                finish()
            }
            .setNegativeButton("Crear Otra") { _, _ ->
                limpiarSeleccion()
            }
            .setCancelable(false)
            .show()
    }

    private fun limpiarSeleccion() {
        selectedPedidos.clear()
        pedidoAdapter.seleccionados.clear()
        pedidoAdapter.notifyDataSetChanged()
        actualizarContadorSeleccionados()

        // Recargar pedidos
        viewModel.cargarPedidosPendientes()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}