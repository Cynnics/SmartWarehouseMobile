package com.smartwarehouse.mobile.ui.perfil

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.ui.login.LoginActivity
import com.smartwarehouse.mobile.ui.MainViewModel
import kotlin.getValue

class PerfilActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        val imgPerfil = findViewById<ImageView>(R.id.imgPerfil)
        val btnEditar = findViewById<Button>(R.id.btnEditar)
        val btnCerrar = findViewById<Button>(R.id.btnCerrarSesion)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

        findViewById<TextView>(R.id.tvNombre).text = prefs.getString("nombre", "Sin nombre")
        findViewById<TextView>(R.id.tvCorreo).text = prefs.getString("email", "Sin correo")
        findViewById<TextView>(R.id.tvRol).text = prefs.getString("rol", "Sin rol")
        findViewById<TextView>(R.id.tvTelefono).text = "Teléfono: " + prefs.getString("telefono", "No disponible")


        btnEditar.setOnClickListener {
            // En el futuro abrirá EditarPerfilActivity
            showToast("Función de edición próximamente...")
        }

        btnCerrar.setOnClickListener {
            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            prefs.edit().clear().apply() // 🔥 Limpia la sesión

            viewModel.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

    }

    private fun showToast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}