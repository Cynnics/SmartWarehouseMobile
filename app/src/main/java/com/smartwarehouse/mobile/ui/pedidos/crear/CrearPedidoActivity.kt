package com.smartwarehouse.mobile.ui.pedidos.crear

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smartwarehouse.mobile.ui.catalogo.CatalogoActivity

/**
 * Activity que redirige al catálogo de productos
 * Sirve como alias para mantener la claridad en la navegación
 */
class CrearPedidoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, CatalogoActivity::class.java)
        startActivity(intent)
        finish()
    }
}