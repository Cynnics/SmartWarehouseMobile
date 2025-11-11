package com.smartwarehouse.mobile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartwarehouse.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnVerPedidos = findViewById<Button>(R.id.btnVerPedidos)
        val tvPedidos = findViewById<TextView>(R.id.tvPedidos)

        btnVerPedidos.setOnClickListener {
            Toast.makeText(this, "Abriendo pedidos...", Toast.LENGTH_SHORT).show()
            tvPedidos.text = "Aquí se mostrarán los pedidos."
            tvPedidos.visibility = View.VISIBLE
        }
    }
}
