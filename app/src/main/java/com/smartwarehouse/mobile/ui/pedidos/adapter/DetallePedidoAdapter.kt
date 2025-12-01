package com.smartwarehouse.mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.data.model.response.DetallePedidoResponse

class DetallePedidoAdapter : ListAdapter<DetallePedidoResponse, DetallePedidoAdapter.DetalleViewHolder>(DetalleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetalleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detalle_pedido, parent, false)
        return DetalleViewHolder(view)
    }

    override fun onBindViewHolder(holder: DetalleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DetalleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductoId: TextView = itemView.findViewById(R.id.tvProductoId)
        private val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad)
        private val tvSubtotal: TextView = itemView.findViewById(R.id.tvSubtotal)

        fun bind(detalle: DetallePedidoResponse) {
            tvProductoId.text = "Producto #${detalle.idProducto}"
            tvCantidad.text = "x ${detalle.cantidad}"
            tvSubtotal.text = String.format("%.2f €", detalle.subtotal)
        }
    }

    class DetalleDiffCallback : DiffUtil.ItemCallback<DetallePedidoResponse>() {
        override fun areItemsTheSame(oldItem: DetallePedidoResponse, newItem: DetallePedidoResponse): Boolean {
            return oldItem.idDetalle == newItem.idDetalle
        }

        override fun areContentsTheSame(oldItem: DetallePedidoResponse, newItem: DetallePedidoResponse): Boolean {
            return oldItem == newItem
        }
    }
}