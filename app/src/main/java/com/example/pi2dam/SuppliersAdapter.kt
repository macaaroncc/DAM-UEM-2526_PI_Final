package com.example.pi2dam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.model.Supplier

class SuppliersAdapter(
    private val onClick: (Supplier) -> Unit
) : RecyclerView.Adapter<SuppliersAdapter.VH>() {

    private val items = mutableListOf<Supplier>()

    fun submit(newItems: List<Supplier>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_supplier, parent, false)
        return VH(v, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View, private val onClick: (Supplier) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
        private val tvSubtitle = itemView.findViewById<TextView>(R.id.tvSubtitle)

        fun bind(s: Supplier) {
            tvTitle.text = s.name
            tvSubtitle.text = "ID: ${s.id.take(8)}"
            itemView.setOnClickListener { onClick(s) }
        }
    }
}
