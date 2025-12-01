package com.smartwarehouse.mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.data.model.response.Ruta
import com.smartwarehouse.mobile.utils.toDate
import com.smartwarehouse.mobile.utils.toFormattedString

class RutaAdapter(
    private val onRutaClick: (Ruta) -> Unit
) : ListAdapter<Ruta, RutaAdapter.RutaViewHolder>(RutaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ruta, parent, false)
        return RutaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RutaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RutaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardRuta)
        private val tvIdRuta: TextView = itemView.findViewById(R.id.tvIdRuta)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFechaRuta)
        private val tvDistancia: TextView = itemView.findViewById(R.id.tvDistancia)
        private val tvDuracion: TextView = itemView.findViewById(R.id.tvDuracion)
        private val estadoIndicator: View = itemView.findViewById(R.id.estadoIndicator)

        fun bind(ruta: Ruta) {
            tvIdRuta.text = "Ruta #${ruta.id}"
            tvEstado.text = ruta.getEstadoTexto()

            // Formatear fecha
            val fecha = ruta.fechaRuta.toDate()?.toFormattedString("dd/MM/yyyy")
                ?: ruta.fechaRuta
            tvFecha.text = fecha

            tvDistancia.text = ruta.getDistanciaTexto()
            tvDuracion.text = ruta.getDuracionTexto()

            // Color del indicador según estado
            estadoIndicator.setBackgroundColor(ruta.getEstadoColor())

            // Click listener
            cardView.setOnClickListener {
                onRutaClick(ruta)
            }
        }
    }

    class RutaDiffCallback : DiffUtil.ItemCallback<Ruta>() {
        override fun areItemsTheSame(oldItem: Ruta, newItem: Ruta): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Ruta, newItem: Ruta): Boolean {
            return oldItem == newItem
        }
    }
}