package com.smartwarehouse.mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.data.model.ItemCarrito

class CarritoAdapter(
    private val onIncrementar: (Int) -> Unit,
    private val onDecrementar: (Int) -> Unit,
    private val onEliminar: (Int) -> Unit
) : ListAdapter<ItemCarrito, CarritoAdapter.CarritoViewHolder>(CarritoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrito, parent, false)
        return CarritoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CarritoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        private val tvPrecioUnitario: TextView = itemView.findViewById(R.id.tvPrecioUnitario)
        private val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidad)
        private val tvSubtotal: TextView = itemView.findViewById(R.id.tvSubtotal)
        private val btnMenos: ImageButton = itemView.findViewById(R.id.btnMenos)
        private val btnMas: ImageButton = itemView.findViewById(R.id.btnMas)
        private val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminar)

        fun bind(item: ItemCarrito) {
            tvNombre.text = item.producto.nombre
            tvPrecioUnitario.text = String.format("%.2f €/ud", item.producto.precio)
            tvCantidad.text = item.cantidad.toString()
            tvSubtotal.text = String.format("%.2f €", item.getSubtotal())

            btnMenos.isEnabled = item.cantidad > 1
            btnMenos.alpha = if (item.cantidad > 1) 1.0f else 0.5f

            btnMas.isEnabled = item.cantidad < item.producto.stock
            btnMas.alpha = if (item.cantidad < item.producto.stock) 1.0f else 0.5f

            btnMenos.setOnClickListener {
                if (item.cantidad > 1) {
                    onDecrementar(item.producto.idProducto)
                }
            }

            btnMas.setOnClickListener {
                if (item.cantidad < item.producto.stock) {
                    onIncrementar(item.producto.idProducto)
                }
            }

            btnEliminar.setOnClickListener {
                onEliminar(item.producto.idProducto)
            }
        }
    }

    class CarritoDiffCallback : DiffUtil.ItemCallback<ItemCarrito>() {
        override fun areItemsTheSame(oldItem: ItemCarrito, newItem: ItemCarrito): Boolean {
            return oldItem.producto.idProducto == newItem.producto.idProducto
        }

        override fun areContentsTheSame(oldItem: ItemCarrito, newItem: ItemCarrito): Boolean {
            return oldItem.cantidad == newItem.cantidad &&
                    oldItem.getSubtotal() == newItem.getSubtotal()
        }

    }
}