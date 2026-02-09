package com.example.pi2dam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.model.EmployeeProfile

class UsersAdapter(
    private val onClick: (EmployeeProfile) -> Unit
) : RecyclerView.Adapter<UsersAdapter.VH>() {

    private val items = mutableListOf<EmployeeProfile>()

    fun submit(newItems: List<EmployeeProfile>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_user, parent, false)
        return VH(v, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View, private val onClick: (EmployeeProfile) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle = itemView.findViewById<TextView>(R.id.tvTitle)
        private val tvSubtitle = itemView.findViewById<TextView>(R.id.tvSubtitle)

        fun bind(u: EmployeeProfile) {
            tvTitle.text = if (u.name.isBlank()) u.email else u.name
            val status = if (u.active) "activo" else "inactivo"
            tvSubtitle.text = "${u.email} · ${u.role} · $status"

            itemView.setOnClickListener { onClick(u) }
        }
    }
}
