package com.smartwarehouse.mobile.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartwarehouse.R
import com.smartwarehouse.mobile.RutaDetalleActivity
import com.smartwarehouse.mobile.model.Ruta

class RutaAdapter(private val rutas: List<Ruta>) :
    RecyclerView.Adapter<RutaAdapter.RutaViewHolder>() {

    class RutaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIdRuta: TextView = itemView.findViewById(R.id.tvIdRuta)
        val tvRepartidor: TextView = itemView.findViewById(R.id.tvRepartidor)
        val tvOrigen: TextView = itemView.findViewById(R.id.tvOrigen)
        val tvDestino: TextView = itemView.findViewById(R.id.tvDestino)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ruta, parent, false)
        return RutaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RutaViewHolder, position: Int) {
        val ruta = rutas[position]

        holder.tvIdRuta.text = "Ruta #${ruta.id}"
        holder.tvRepartidor.text = "Repartidor: ${ruta.repartidor}"
        holder.tvOrigen.text = "Origen: ${ruta.origen}"
        holder.tvDestino.text = "Destino: ${ruta.destino}"
        holder.tvEstado.text = "Estado: ${ruta.estado}"

        // Abrir la pantalla de detalle al hacer clic
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, RutaDetalleActivity::class.java).apply {
                putExtra("idRuta", ruta.id)
                putExtra("repartidor", ruta.repartidor)
                putExtra("origen", ruta.origen)
                putExtra("destino", ruta.destino)
                putExtra("estado", ruta.estado)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = rutas.size
}
