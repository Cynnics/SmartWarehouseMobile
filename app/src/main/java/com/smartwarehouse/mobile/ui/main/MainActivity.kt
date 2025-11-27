package com.smartwarehouse.mobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.smartwarehouse.mobile.ui.login.LoginActivity
import com.smartwarehouse.mobile.ui.main.MainViewModel
import com.smartwarehouse.mobile.ui.pedidos.PedidosActivity
import com.smartwarehouse.mobile.ui.perfil.PerfilActivity
import com.smartwarehouse.mobile.ui.rutas.RutasActivity

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var welcomeText: TextView
    private lateinit var roleText: TextView
    private lateinit var btnPedidos: Button
    private lateinit var btnRutas: Button
    private lateinit var btnPerfil: Button
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupObservers()
        setupListeners()
        configureMenuByRole()
    }

    private fun initializeViews() {
        welcomeText = findViewById(R.id.welcomeText)
        roleText = findViewById(R.id.roleText)
        btnPedidos = findViewById(R.id.btnPedidos)
        btnRutas = findViewById(R.id.btnRutas)
        btnPerfil = findViewById(R.id.btnPerfil)
        btnLogout = findViewById(R.id.btnLogout)
    }

    private fun setupObservers() {
        viewModel.userName.observe(this) { name ->
            welcomeText.text = "Bienvenido, $name"
        }

        viewModel.userRole.observe(this) { role ->
            roleText.text = "Rol: ${getRoleDisplayName(role)}"
        }
    }

    private fun setupListeners() {
        btnPedidos.setOnClickListener {
            startActivity(Intent(this, PedidosActivity::class.java))
        }

        btnRutas.setOnClickListener {
            startActivity(Intent(this, RutasActivity::class.java))
        }

        btnPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        btnLogout.setOnClickListener {
            viewModel.logout()
            navigateToLogin()
        }
    }

    private fun configureMenuByRole() {
        when {
            viewModel.isRepartidor() -> {
                // Repartidores ven rutas y pedidos
                btnPedidos.isEnabled = true
                btnRutas.isEnabled = true
            }
            viewModel.isCliente() -> {
                // Clientes solo ven sus pedidos
                btnPedidos.isEnabled = true
                btnRutas.isEnabled = false
                btnRutas.alpha = 0.5f
            }
            else -> {
                // Otros roles ven todo
                btnPedidos.isEnabled = true
                btnRutas.isEnabled = true
            }
        }
    }

    private fun getRoleDisplayName(role: String): String {
        return when (role.lowercase()) {
            "admin" -> "Administrador"
            "empleado" -> "Empleado"
            "repartidor" -> "Repartidor"
            "cliente" -> "Cliente"
            else -> "Sin rol"
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}