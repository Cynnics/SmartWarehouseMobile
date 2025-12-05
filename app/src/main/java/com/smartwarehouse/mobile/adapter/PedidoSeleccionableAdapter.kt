package com.smartwarehouse.mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.data.model.response.Pedido
import com.smartwarehouse.mobile.utils.toDate
import com.smartwarehouse.mobile.utils.toFormattedString

class PedidoSeleccionableAdapter(
    private val onPedidoToggle: (Pedido, Boolean) -> Unit
) : ListAdapter<Pedido, PedidoSeleccionableAdapter.PedidoViewHolder>(PedidoDiffCallback()) {

    val seleccionados = mutableSetOf<Int>()

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
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxPedido)

        fun bind(pedido: Pedido) {
            // Otros campos igual que antes...
            val tvIdPedido: TextView = itemView.findViewById(R.id.tvIdPedido)
            val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
            val tvFecha: TextView = itemView.findViewById(R.id.tvFechaPedido)
            val estadoIndicator: View = itemView.findViewById(R.id.estadoIndicator)

            tvIdPedido.text = "Pedido #${pedido.id}"
            tvEstado.text = pedido.getEstadoTexto()

            val fecha = pedido.fechaPedido.toDate()?.toFormattedString("dd/MM/yyyy HH:mm")
                ?: pedido.fechaPedido
            tvFecha.text = fecha

            estadoIndicator.setBackgroundColor(pedido.getEstadoColor())

            // Aplicar estado
            checkBox.isChecked = seleccionados.contains(pedido.id)

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) seleccionados.add(pedido.id)
                else seleccionados.remove(pedido.id)

                // 🔥 Notifica a la Activity
                onPedidoToggle(pedido, isChecked)
            }

            cardView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
            }
        }
    }

    class PedidoDiffCallback : DiffUtil.ItemCallback<Pedido>() {
        override fun areItemsTheSame(oldItem: Pedido, newItem: Pedido) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Pedido, newItem: Pedido) = oldItem == newItem
    }
}
