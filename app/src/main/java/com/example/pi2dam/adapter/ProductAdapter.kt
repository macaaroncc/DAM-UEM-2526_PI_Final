package com.example.pi2dam.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.R
import com.example.pi2dam.model.Product

class ProductAdapter(
    private val onClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvDetails: TextView = view.findViewById(R.id.tvDetails)
        private val tvStockPrice: TextView = view.findViewById(R.id.tvStockPrice)

        fun bind(product: Product) {
            tvName.text = product.name
            tvDetails.text = "${product.sku} . ${product.location}"
            tvStockPrice.text = "${product.stock} uds . $${product.price}"

            val isLowStock = product.stock <= product.lowStockThreshold
            ivIcon.setImageResource(
                if (isLowStock) android.R.drawable.ic_dialog_alert
                else android.R.drawable.ic_menu_agenda
            )

            itemView.setOnClickListener { onClick(product) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(old: Product, new: Product) = old.id == new.id
            override fun areContentsTheSame(old: Product, new: Product) = old == new
        }
    }
}
