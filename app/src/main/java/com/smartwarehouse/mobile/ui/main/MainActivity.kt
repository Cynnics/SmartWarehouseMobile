package com.smartwarehouse.mobile.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.smartwarehouse.mobile.ui.login.LoginActivity
import com.smartwarehouse.mobile.ui.main.MainViewModel
import com.smartwarehouse.mobile.ui.main.cliente.ClienteMainActivity
import com.smartwarehouse.mobile.ui.main.repartidor.RepartidorMainActivity

/**
 * MainActivity actúa como ROUTER
 * Detecta el rol del usuario y redirige a la interfaz correspondiente
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No setContentView porque esta Activity solo redirige
        routeToAppropriateActivity()
    }


    private fun routeToAppropriateActivity() {
        val userRole = viewModel.userRole.value

        val intent = when (userRole?.lowercase()) {
            "repartidor" -> Intent(this, RepartidorMainActivity::class.java)
            "cliente" -> Intent(this, ClienteMainActivity::class.java)
            "admin", "empleado" -> {
                // Por ahora redirige a la app de escritorio
                // Podrías crear AdminMainActivity si lo necesitas
                Intent(this, ClienteMainActivity::class.java) // Temporal
            }
            else -> {
                // Sin rol válido, volver al login
                Intent(this, LoginActivity::class.java)
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}