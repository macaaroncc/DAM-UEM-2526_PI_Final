package com.example.pi2dam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.model.Product
import com.google.android.material.button.MaterialButton

class PickProductsAdapter : RecyclerView.Adapter<PickProductsAdapter.VH>() {

    private val items = mutableListOf<Product>()
    private val qtyById = mutableMapOf<String, Long>()

    fun submit(newItems: List<Product>) {
        items.clear()
        items.addAll(newItems)
        // mantenemos cantidades existentes si el producto sigue en la lista
        val keep = qtyById.toMap()
        qtyById.clear()
        newItems.forEach { p -> qtyById[p.id] = keep[p.id] ?: 0L }
        notifyDataSetChanged()
    }

    fun getQty(productId: String): Long = qtyById[productId] ?: 0L

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_pick_product, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.bind(p, getQty(p.id)) { newQty ->
            qtyById[p.id] = newQty
        }
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
        private val tvSubtitle = itemView.findViewById<TextView>(R.id.tvSubtitle)
        private val tvQty = itemView.findViewById<TextView>(R.id.tvQty)
        private val btnMinus = itemView.findViewById<MaterialButton>(R.id.btnMinus)
        private val btnPlus = itemView.findViewById<MaterialButton>(R.id.btnPlus)

        private var currentQty: Long = 0L

        fun bind(p: Product, qty: Long, onQtyChange: (Long) -> Unit) {
            tvTitle.text = p.name
            tvSubtitle.text = "stock: ${p.stock} · precio: ${p.price}"

            currentQty = qty
            tvQty.text = currentQty.toString()

            btnMinus.setOnClickListener {
                currentQty = (currentQty - 1).coerceAtLeast(0)
                tvQty.text = currentQty.toString()
                onQtyChange(currentQty)
            }
            btnPlus.setOnClickListener {
                currentQty = (currentQty + 1).coerceAtMost(p.stock)
                tvQty.text = currentQty.toString()
                onQtyChange(currentQty)
            }
        }
    }
}
