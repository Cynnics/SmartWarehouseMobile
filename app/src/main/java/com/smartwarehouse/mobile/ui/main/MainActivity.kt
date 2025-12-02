package com.smartwarehouse.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.smartwarehouse.mobile.ui.login.LoginActivity
import com.smartwarehouse.mobile.ui.main.MainViewModel
import com.smartwarehouse.mobile.ui.main.cliente.ClienteMainActivity
import com.smartwarehouse.mobile.ui.main.repartidor.RepartidorMainActivity
import com.smartwarehouse.mobile.utils.SessionManager
/**
 * MainActivity actúa como ROUTER
 * Detecta el rol del usuario y redirige a la interfaz correspondiente
 */
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val sessionManager by lazy { SessionManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!sessionManager.isLoggedIn() || sessionManager.isTokenExpired())
            logoutAndGoToLogin()
        else
            routeToAppropriateActivity()
    }

    private fun logoutAndGoToLogin() {
        sessionManager.clearSession()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
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