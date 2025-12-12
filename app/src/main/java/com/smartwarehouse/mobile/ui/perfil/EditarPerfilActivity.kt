package com.smartwarehouse.mobile.ui.perfil

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.utils.NetworkResult
import com.smartwarehouse.mobile.utils.showToast

class EditarPerfilActivity : AppCompatActivity() {

    private val viewModel: PerfilViewModel by viewModels()

    private lateinit var etNombre: TextInputEditText
    private lateinit var etTelefono: TextInputEditText
    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil)

        setupToolbar()
        initializeViews()
        loadCurrentData()
        setupObservers()
        setupListeners()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Editar Perfil"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun initializeViews() {
        etNombre = findViewById(R.id.etNombre)
        etTelefono = findViewById(R.id.etTelefono)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnCancelar = findViewById(R.id.btnCancelar)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun loadCurrentData() {
        val nombreActual = intent.getStringExtra("NOMBRE_ACTUAL")
        val telefonoActual = intent.getStringExtra("TELEFONO_ACTUAL")

        etNombre.setText(nombreActual)
        etTelefono.setText(telefonoActual)
    }

    private fun setupObservers() {
        viewModel.actualizarPerfilResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    showToast("✅ Perfil actualizado correctamente")
                    setResult(RESULT_OK)
                    finish()
                }
                is NetworkResult.Error -> {
                    showToast(result.message ?: "Error al actualizar perfil")
                }
                is NetworkResult.Loading -> {
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnGuardar.isEnabled = !isLoading
            btnCancelar.isEnabled = !isLoading
            etNombre.isEnabled = !isLoading
            etTelefono.isEnabled = !isLoading
        }
    }

    private fun setupListeners() {
        btnGuardar.setOnClickListener {
            guardarCambios()
        }

        btnCancelar.setOnClickListener {
            finish()
        }
    }

    private fun guardarCambios() {
        val nombre = etNombre.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()

        if (nombre.isEmpty()) {
            etNombre.error = "El nombre es obligatorio"
            etNombre.requestFocus()
            return
        }

        if (nombre.length < 3) {
            etNombre.error = "El nombre debe tener al menos 3 caracteres"
            etNombre.requestFocus()
            return
        }

        if (telefono.isNotEmpty() && telefono.length != 9) {
            etTelefono.error = "El teléfono debe tener 9 dígitos"
            etTelefono.requestFocus()
            return
        }

        viewModel.actualizarPerfil(nombre, telefono)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}