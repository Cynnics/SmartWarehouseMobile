package com.smartwarehouse.mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartwarehouse.R
import com.smartwarehouse.mobile.model.Ruta

class RutaAdapter(private val rutas: List<Ruta>) :
    RecyclerView.Adapter<RutaAdapter.RutaViewHolder>() {

    inner class RutaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIdRuta: TextView = view.findViewById(R.id.tvIdRuta)
        val tvRepartidor: TextView = view.findViewById(R.id.tvRepartidor)
        val tvOrigen: TextView = view.findViewById(R.id.tvOrigen)
        val tvDestino: TextView = view.findViewById(R.id.tvDestino)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutaViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ruta, parent, false)
        return RutaViewHolder(vista)
    }

    override fun onBindViewHolder(holder: RutaViewHolder, position: Int) {
        val ruta = rutas[position]
        holder.tvIdRuta.text = "Ruta #${ruta.id}"
        holder.tvRepartidor.text = "Repartidor: ${ruta.repartidor}"
        holder.tvOrigen.text = "Origen: ${ruta.origen}"
        holder.tvDestino.text = "Destino: ${ruta.destino}"
        holder.tvEstado.text = "Estado: ${ruta.estado}"
    }

    override fun getItemCount(): Int = rutas.size
}
