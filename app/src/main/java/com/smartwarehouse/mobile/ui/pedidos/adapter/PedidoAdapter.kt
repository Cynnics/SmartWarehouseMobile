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
import com.smartwarehouse.mobile.data.model.response.Pedido
import com.smartwarehouse.mobile.utils.toDate
import com.smartwarehouse.mobile.utils.toFormattedString

class PedidoAdapter(
    private val onPedidoClick: (Pedido) -> Unit
) : ListAdapter<Pedido, PedidoAdapter.PedidoViewHolder>(PedidoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido, parent, false)
        return PedidoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PedidoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardPedido)
        private val tvIdPedido: TextView = itemView.findViewById(R.id.tvIdPedido)
        private val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFechaPedido)
        private val estadoIndicator: View = itemView.findViewById(R.id.estadoIndicator)

        fun bind(pedido: Pedido) {
            tvIdPedido.text = "Pedido #${pedido.id}"
            tvEstado.text = pedido.getEstadoTexto()

            // Formatear fecha
            val fecha = pedido.fechaPedido.toDate()?.toFormattedString("dd/MM/yyyy HH:mm")
                ?: pedido.fechaPedido
            tvFecha.text = fecha

            // Color del indicador según estado
            estadoIndicator.setBackgroundColor(pedido.getEstadoColor())

            // Click listener
            cardView.setOnClickListener {
                onPedidoClick(pedido)
            }
        }
    }

    class PedidoDiffCallback : DiffUtil.ItemCallback<Pedido>() {
        override fun areItemsTheSame(oldItem: Pedido, newItem: Pedido): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Pedido, newItem: Pedido): Boolean {
            return oldItem == newItem
        }
    }
}