package com.smartwarehouse.mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smartwarehouse.R
import com.smartwarehouse.mobile.model.Pedido

class PedidoAdapter(private val listaPedidos: List<Pedido>) :
    RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder>() {

    inner class PedidoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIdPedido: TextView = itemView.findViewById(R.id.tvIdPedido)
        val tvCliente: TextView = itemView.findViewById(R.id.tvCliente)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pedido, parent, false)
        return PedidoViewHolder(vista)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        val pedido = listaPedidos[position]
        holder.tvIdPedido.text = "Pedido #${pedido.id}"
        holder.tvCliente.text = "Cliente: ${pedido.cliente}"
        holder.tvFecha.text = "Fecha: ${pedido.fecha}"
        holder.tvEstado.text = "Estado: ${pedido.estado}"
    }

    override fun getItemCount(): Int = listaPedidos.size
}

