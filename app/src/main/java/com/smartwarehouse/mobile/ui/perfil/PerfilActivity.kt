package com.smartwarehouse.mobile.ui.perfil

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.domain.model.Usuario
import com.smartwarehouse.mobile.ui.login.LoginActivity

class PerfilActivity : AppCompatActivity() {

    private lateinit var usuario: Usuario

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        val imgPerfil = findViewById<ImageView>(R.id.imgPerfil)
        val tvNombre = findViewById<TextView>(R.id.tvNombre)
        val tvCorreo = findViewById<TextView>(R.id.tvCorreo)
        val tvRol = findViewById<TextView>(R.id.tvRol)
        val tvTelefono = findViewById<TextView>(R.id.tvTelefono)
        val btnEditar = findViewById<Button>(R.id.btnEditar)
        val btnCerrar = findViewById<Button>(R.id.btnCerrarSesion)

        // 🔹 Simulamos datos del usuario logueado
        usuario = Usuario(
            id = 1,
            nombre = "Juan Pérez",
            correo = "juanperez@empresa.com",
            rol = "Repartidor",
            telefono = "+34 600 123 456"
        )

        tvNombre.text = usuario.nombre
        tvCorreo.text = usuario.correo
        tvRol.text = usuario.rol
        tvTelefono.text = "Teléfono: ${usuario.telefono}"

        btnEditar.setOnClickListener {
            // En el futuro abrirá EditarPerfilActivity
            showToast("Función de edición próximamente...")
        }

        btnCerrar.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun showToast(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}