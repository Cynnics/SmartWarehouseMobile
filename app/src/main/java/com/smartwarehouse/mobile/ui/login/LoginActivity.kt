package com.smartwarehouse.mobile.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.ui.admin.AdminEmpleadoMainActivity
import com.smartwarehouse.mobile.ui.cliente.ClienteMainActivity
import com.smartwarehouse.mobile.ui.repartidor.RepartidorMainActivity
import com.smartwarehouse.mobile.utils.NetworkResult

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var loginButton: android.widget.Button
    private lateinit var progressBar: android.widget.ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si ya hay sesión activa
        if (viewModel.isLoggedIn()) {
            routeToAppropriateActivity()
            return
        }

        setContentView(R.layout.activity_login)

        initializeViews()
        setupObservers()
        setupListeners()
    }

    private fun initializeViews() {
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupObservers() {
        // Observer para el resultado del login
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    val usuario = result.data?.usuario

                    if (usuario != null) {
                        // 🔥 Guardar datos del usuario
                        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                        prefs.edit()
                            .putString("nombre", usuario.nombre)
                            .putString("email", usuario.email)
                            .putString("rol", usuario.rol)
                            .putString("telefono", usuario.telefono)
                            .apply()
                    }
                    Toast.makeText(this, "Bienvenido ${result.data?.usuario?.nombre}", Toast.LENGTH_SHORT).show()
                    routeToAppropriateActivity()
                }
                is NetworkResult.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
                is NetworkResult.Loading -> {
                    // El estado de carga se maneja en isLoading
                }
            }
        }
    }

    private fun setupListeners() {
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            viewModel.login(email, password)
        }
    }

    private fun routeToAppropriateActivity() {
        val userRole = viewModel.getUserRole()

        val intent = when (userRole?.lowercase()) {
            "repartidor" -> Intent(this, RepartidorMainActivity::class.java)
            "cliente" -> Intent(this, ClienteMainActivity::class.java)
            "admin", "empleado" -> Intent(this, AdminEmpleadoMainActivity::class.java)
            else -> {
                // Rol desconocido, cerrar sesión y quedarse en login
                viewModel.logout()
                Toast.makeText(this, "Rol de usuario no válido", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Limpiar stack de actividades y abrir la nueva
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}