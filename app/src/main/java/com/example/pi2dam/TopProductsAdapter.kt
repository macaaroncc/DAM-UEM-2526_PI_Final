package com.example.pi2dam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator

class TopProductsAdapter : RecyclerView.Adapter<TopProductsAdapter.VH>() {

    data class Row(
        val name: String,
        val qty: Long,
        val pct: Int // 0..100
    )

    private val items = ArrayList<Row>()

    fun submit(rows: List<Row>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_top_product, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvQty: TextView = itemView.findViewById(R.id.tvQty)
        private val pi: LinearProgressIndicator = itemView.findViewById(R.id.pi)

        fun bind(r: Row) {
            tvName.text = r.name
            tvQty.text = "${r.qty} uds"
            pi.setProgressCompat(r.pct.coerceIn(0, 100), true)
        }
    }
}
