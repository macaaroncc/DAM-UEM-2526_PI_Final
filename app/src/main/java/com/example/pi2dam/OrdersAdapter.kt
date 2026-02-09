package com.example.pi2dam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.model.OrderStatus
import com.google.android.material.button.MaterialButton

class OrdersAdapter(
    private val onCancel: (orderId: String) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.VH>() {

    data class OrderRow(
        val id: String,
        val status: String,
        val createdByUid: String,
        val itemCount: Int,
        val createdAtMs: Long
    )

    private val items = mutableListOf<OrderRow>()
    private var currentUid: String = ""
    private var isAdmin: Boolean = false

    fun setPermissions(currentUid: String, isAdmin: Boolean) {
        this.currentUid = currentUid
        this.isAdmin = isAdmin
        notifyDataSetChanged()
    }

    fun submit(newItems: List<OrderRow>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_order, parent, false)
        return VH(v, onCancel)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val o = items[position]
        val canCancel = (isAdmin || o.createdByUid == currentUid) && o.status != OrderStatus.CANCELLED
        holder.bind(o, canCancel)
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View, private val onCancel: (String) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
        private val tvSubtitle = itemView.findViewById<TextView>(R.id.tvSubtitle)
        private val btnCancel = itemView.findViewById<MaterialButton>(R.id.btnCancel)

        fun bind(o: OrderRow, canCancel: Boolean) {
            tvTitle.text = "Pedido ${o.id.take(8)} · ${o.status}"
            tvSubtitle.text = "items: ${o.itemCount} · creado por: ${o.createdByUid.take(6)}"

            btnCancel.visibility = if (canCancel) View.VISIBLE else View.GONE
            btnCancel.setOnClickListener { onCancel(o.id) }
        }
    }
}
