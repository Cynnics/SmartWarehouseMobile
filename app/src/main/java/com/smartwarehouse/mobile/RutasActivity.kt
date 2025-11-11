package com.smartwarehouse.mobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smartwarehouse.R

class RutasActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rutas)
        setSupportActionBar(findViewById(R.id.toolbar))

    }
}