package com.smartwarehouse.mobile.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.smartwarehouse.mobile.ui.MainActivity
import com.smartwarehouse.mobile.R
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
            navigateToMain()
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
                    navigateToMain()
                }
                is NetworkResult.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
                is NetworkResult.Loading -> {
                    // El estado de carga se maneja en isLoading
                }
            }
        }

        // Observer para el estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            loginButton.isEnabled = !isLoading
            emailInput.isEnabled = !isLoading
            passwordInput.isEnabled = !isLoading
        }
    }

    private fun setupListeners() {
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            viewModel.login(email, password)
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}