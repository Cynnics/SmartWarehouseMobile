package com.smartwarehouse.mobile.ui.perfil

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.ui.login.LoginActivity

class PerfilActivity : AppCompatActivity() {

    private val viewModel: PerfilViewModel by viewModels()

    private lateinit var imgPerfil: ImageView
    private lateinit var tvNombre: TextView
    private lateinit var tvCorreo: TextView
    private lateinit var tvRol: TextView
    private lateinit var tvTelefono: TextView
    private lateinit var tvIdUsuario: TextView
    private lateinit var btnEditar: Button
    private lateinit var btnCerrarSesion: Button

    companion object {
        const val REQUEST_EDITAR_PERFIL = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        setupToolbar()
        initializeViews()
        setupObservers()
        setupListeners()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Mi Perfil"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun initializeViews() {
        imgPerfil = findViewById(R.id.imgPerfil)
        tvNombre = findViewById(R.id.tvNombre)
        tvCorreo = findViewById(R.id.tvCorreo)
        tvRol = findViewById(R.id.tvRol)
        tvTelefono = findViewById(R.id.tvTelefono)
        tvIdUsuario = findViewById(R.id.tvIdUsuario)
        btnEditar = findViewById(R.id.btnEditar)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
    }

    private fun setupObservers() {
        viewModel.usuario.observe(this) { usuario ->
            tvNombre.text = usuario.nombre
            tvCorreo.text = usuario.email
            tvRol.text = viewModel.getRoleDisplayName()
            tvTelefono.text = usuario.telefono
            tvIdUsuario.text = "ID: ${usuario.idUsuario}"
        }
    }

    private fun setupListeners() {
        btnEditar.setOnClickListener {
            val usuario = viewModel.usuario.value
            if (usuario != null) {
                val intent = Intent(this, EditarPerfilActivity::class.java).apply {
                    putExtra("NOMBRE_ACTUAL", usuario.nombre)
                    putExtra("TELEFONO_ACTUAL", usuario.telefono)
                }
                startActivityForResult(intent, REQUEST_EDITAR_PERFIL)
            }
        }

        btnCerrarSesion.setOnClickListener {
            mostrarDialogoCerrarSesion()
        }
    }

    private fun mostrarDialogoCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Cerrar Sesión") { _, _ ->
                cerrarSesion()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cerrarSesion() {
        viewModel.logout()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_EDITAR_PERFIL && resultCode == RESULT_OK) {
            // Recargar datos del perfil
            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            tvNombre.text = prefs.getString("nombre", "Sin nombre")
            tvTelefono.text = prefs.getString("telefono", "Sin teléfono")
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}