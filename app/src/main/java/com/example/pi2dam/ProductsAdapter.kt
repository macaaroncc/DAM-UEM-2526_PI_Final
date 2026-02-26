package com.example.pi2dam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.model.Product
import java.util.Locale

class ProductsAdapter(
    private val onClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.VH>() {
    data class Row(
        val product: Product,
        val supplierName: String
    )

    private val items = mutableListOf<Row>()

    fun submit(newItems: List<Product>, suppliersById: Map<String, String>) {
        items.clear()
        items.addAll(
            newItems.map { p ->
                Row(
                    product = p,
                    supplierName = suppliersById[p.supplierId].orEmpty()
                )
            }
        )
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_product, parent, false)
        return VH(v, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View, private val onClick: (Product) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
        private val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
        private val tvSubtitle = itemView.findViewById<TextView>(R.id.tvSubtitle)
        fun bind(row: Row) {
            val p = row.product
            tvTitle.text = if (p.sku.isBlank()) p.name else "${p.name} (${p.sku})"
            val isLowStock = p.stock <= p.lowStockThreshold
            tvStatus.text = if (isLowStock) {
                itemView.context.getString(R.string.products_stock_low_badge)
            } else {
                itemView.context.getString(R.string.products_stock_ok_badge)
            }
            tvStatus.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (isLowStock) R.color.stock_low else R.color.stock_ok
                )
            )
            val supplier = row.supplierName.ifBlank {
                itemView.context.getString(R.string.products_supplier_unknown)
            }
            val location = p.location.ifBlank {
                itemView.context.getString(R.string.products_location_unknown)
            }
            val priceText = String.format(Locale.getDefault(), "%.2f", p.price)
            tvSubtitle.text = itemView.context.getString(
                R.string.products_row_subtitle,
                p.stock,
                priceText,
                location,
                supplier
            )
            itemView.setOnClickListener { onClick(p) }
        }
    }
}
