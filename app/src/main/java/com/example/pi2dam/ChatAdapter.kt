package com.example.pi2dam

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val myUid: () -> String?
) : RecyclerView.Adapter<ChatAdapter.VH>() {

    data class Msg(
        val id: String,
        val senderUid: String,
        val senderName: String,
        val senderRole: String,
        val text: String,
        val createdAt: Date
    )

    private val items = ArrayList<Msg>()

    fun submit(rows: List<Msg>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_chat_message, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], items[position].senderUid == myUid())
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bubble: MaterialCardView = itemView.findViewById(R.id.bubble)
        private val tvMeta: TextView = itemView.findViewById(R.id.tvMeta)
        private val tvText: TextView = itemView.findViewById(R.id.tvText)

        private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(m: Msg, isMe: Boolean) {
            val ctx = itemView.context
            tvMeta.text = "${m.senderName} · ${timeFmt.format(m.createdAt)}"
            tvText.text = m.text

            val lp = bubble.layoutParams as ConstraintLayout.LayoutParams
            if (isMe) {
                lp.startToStart = ConstraintLayout.LayoutParams.UNSET
                lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                bubble.layoutParams = lp

                bubble.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.brand_orange))
                tvMeta.setTextColor(ContextCompat.getColor(ctx, R.color.white))
                tvText.setTextColor(ContextCompat.getColor(ctx, R.color.white))
            } else {
                lp.endToEnd = ConstraintLayout.LayoutParams.UNSET
                lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                bubble.layoutParams = lp

                bubble.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.surface))
                tvMeta.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                tvText.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            }
        }
    }
}
