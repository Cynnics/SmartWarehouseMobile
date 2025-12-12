package com.smartwarehouse.mobile.ui.rutas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.adapter.RutaAdapter

class RutasActivity : AppCompatActivity() {

    private val viewModel: RutasViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView

    private val rutaAdapter = RutaAdapter { ruta ->
        abrirDetalleRuta(ruta.id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rutas)

        setupToolbar()
        initializeViews()
        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            title = "Mis Rutas"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerRutas)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        emptyView = findViewById(R.id.emptyView)
    }

    private fun setupRecyclerView() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RutasActivity)
            adapter = rutaAdapter
        }
    }

    private fun setupObservers() {
        viewModel.rutas.observe(this) { rutas ->

            if (rutas == null || rutas.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                emptyView.text = "No tienes rutas asignadas"
            } else {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                recyclerView.scrollToPosition(0)

                rutaAdapter.submitList(rutas)
            }

            swipeRefresh.isRefreshing = false
            progressBar.visibility = View.GONE
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            progressBar.visibility = View.VISIBLE
            viewModel.sincronizarRutas()
        }
    }

    private fun abrirDetalleRuta(idRuta: Int) {
        val intent = Intent(this, RutaDetalleActivity::class.java)
        intent.putExtra("ID_RUTA", idRuta)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        viewModel.sincronizarRutas()
    }
}