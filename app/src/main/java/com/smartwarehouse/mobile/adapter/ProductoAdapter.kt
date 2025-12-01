package com.smartwarehouse.mobile.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartwarehouse.mobile.R
import com.smartwarehouse.mobile.data.model.response.ProductoResponse

class ProductoAdapter(
    private val onProductoClick: (ProductoResponse) -> Unit,
    private val onAgregarClick: (ProductoResponse) -> Unit
) : ListAdapter<ProductoResponse, ProductoAdapter.ProductoViewHolder>(ProductoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        private val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        private val tvCategoria: TextView = itemView.findViewById(R.id.tvCategoria)
        private val btnAgregar: Button = itemView.findViewById(R.id.btnAgregar)

        fun bind(producto: ProductoResponse) {
            tvNombre.text = producto.nombre
            tvPrecio.text = producto.getPrecioFormateado()
            tvStock.text = producto.getStockTexto()
            tvCategoria.text = producto.categoria ?: "Sin categoría"

            // Configurar botón según stock
            btnAgregar.isEnabled = producto.tieneStock()
            btnAgregar.text = if (producto.tieneStock()) "Agregar al Carrito" else "Sin Stock"

            // Click en el card
            itemView.setOnClickListener {
                onProductoClick(producto)
            }

            // Click en agregar
            btnAgregar.setOnClickListener {
                if (producto.tieneStock()) {
                    onAgregarClick(producto)
                }
            }
        }
    }

    class ProductoDiffCallback : DiffUtil.ItemCallback<ProductoResponse>() {
        override fun areItemsTheSame(oldItem: ProductoResponse, newItem: ProductoResponse): Boolean {
            return oldItem.idProducto == newItem.idProducto
        }

        override fun areContentsTheSame(oldItem: ProductoResponse, newItem: ProductoResponse): Boolean {
            return oldItem == newItem
        }
    }
}