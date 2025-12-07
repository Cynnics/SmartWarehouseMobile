package com.smartwarehouse.mobile.ui.repartidor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.ui.MainViewModel
import com.smartwarehouse.mobile.ui.login.LoginActivity
import com.smartwarehouse.mobile.ui.pedidos.PedidosActivity
import com.smartwarehouse.mobile.ui.perfil.PerfilActivity
import com.smartwarehouse.mobile.ui.rutas.RutasActivity

class RepartidorMainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var tvBienvenida: TextView
    private lateinit var tvSubtitulo: TextView
    private lateinit var btnMisRutas: Button
    private lateinit var btnPedidosAsignados: Button
    private lateinit var btnPerfil: Button
    private lateinit var btnCerrarSesion: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repartidor_main)

        initializeViews()
        setupObservers()
        setupListeners()
    }

    private fun initializeViews() {
        tvBienvenida = findViewById(R.id.tvBienvenida)
        tvSubtitulo = findViewById(R.id.tvSubtitulo)
        btnMisRutas = findViewById(R.id.btnMisRutas)
        btnPedidosAsignados = findViewById(R.id.btnPedidosAsignados)
        btnPerfil = findViewById(R.id.btnPerfil)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
    }

    private fun setupObservers() {
        viewModel.userName.observe(this) { name ->
            tvBienvenida.text = "¡Hola, $name!"
            tvSubtitulo.text = "Repartidor"
        }
    }

    private fun setupListeners() {
        btnMisRutas.setOnClickListener {
            startActivity(Intent(this, RutasActivity::class.java))
        }

        btnPedidosAsignados.setOnClickListener {
            startActivity(Intent(this, PedidosActivity::class.java))
        }

        btnPerfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        btnCerrarSesion.setOnClickListener {
            viewModel.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}