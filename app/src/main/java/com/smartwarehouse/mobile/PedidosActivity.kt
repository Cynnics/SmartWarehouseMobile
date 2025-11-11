package com.smartwarehouse.mobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smartwarehouse.R

class PedidosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedidos)
        setSupportActionBar(findViewById(R.id.toolbar))

    }
}
