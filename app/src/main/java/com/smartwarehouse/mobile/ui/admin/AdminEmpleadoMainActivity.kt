package com.smartwarehouse.mobile.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.ui.login.LoginActivity
import com.smartwarehouse.mobile.ui.login.LoginViewModel
import com.smartwarehouse.mobile.ui.pedidos.PedidosActivity
import com.smartwarehouse.mobile.ui.perfil.PerfilActivity
import com.smartwarehouse.mobile.ui.rutas.AsignarRutaActivity

class AdminEmpleadoMainActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_empleado_main)

        val btnTodosPedidos = findViewById<MaterialButton>(R.id.btnConsultarPedidos)
        val btnAsignarRutas = findViewById<MaterialButton>(R.id.btnAsignarRutas)
        val btnTodasRutas = findViewById<MaterialButton>(R.id.btnTodasRutas)

        val btnPerfil = findViewById<MaterialButton>(R.id.btnPerfil)
        val btnCerrarSesion = findViewById<MaterialButton>(R.id.btnCerrarSesion)

        btnTodosPedidos.setOnClickListener {
            val intent = Intent(this, PedidosActivity::class.java)
            intent.putExtra(PedidosActivity.Companion.EXTRA_TODOS_PEDIDOS, true)
            startActivity(intent)
        }

        btnAsignarRutas.setOnClickListener {
            val intent = Intent(this, AsignarRutaActivity::class.java)
            startActivity(intent)
        }

        btnTodasRutas.setOnClickListener {
            val intent = Intent(this, TodasRutasActivity::class.java)
            startActivity(intent)
        }

        btnPerfil.setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            startActivity(intent)
        }

        btnCerrarSesion.setOnClickListener {
            viewModel.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}