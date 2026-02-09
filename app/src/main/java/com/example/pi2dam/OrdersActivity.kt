package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.model.EmployeeProfile.Companion.ROLE_ADMIN
import com.google.android.material.button.MaterialButton

class OrdersActivity : AppCompatActivity() {

    private var ordersListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val adapter = OrdersAdapter(
        onCancel = { orderId ->
            PiRepository.cancelOrder(orderId)
                .addOnSuccessListener {
                    Toast.makeText(this, "Pedido cancelado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "No se pudo cancelar", Toast.LENGTH_SHORT).show()
                }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_orders)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnCreateOrder).setOnClickListener {
            startActivity(Intent(this, CreateOrderActivity::class.java))
        }

        findViewById<RecyclerView>(R.id.rvOrders).apply {
            layoutManager = LinearLayoutManager(this@OrdersActivity)
            adapter = this@OrdersActivity.adapter
        }
    }

    override fun onStart() {
        super.onStart()
        val current = FirebaseRefs.auth.currentUser
        if (current == null) {
            finish()
            return
        }

        PiRepository.ensureEmployeeAccess(current.uid)
            .addOnSuccessListener { me ->
                Session.setEmployee(me)
                adapter.setPermissions(currentUid = current.uid, isAdmin = me.role == ROLE_ADMIN)

                ordersListener?.remove()
                ordersListener = FirebaseRefs.db.collection(FirebaseRefs.COL_ORDERS)
                    .addSnapshotListener { snap, e ->
                        if (e != null || snap == null) return@addSnapshotListener

                        val items = snap.documents.map { d ->
                            val status = d.getString("status") ?: ""
                            val createdByUid = d.getString("createdByUid") ?: ""
                            val createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L

                            @Suppress("UNCHECKED_CAST")
                            val rawItems = (d.get("items") as? List<Map<String, Any?>>).orEmpty()

                            OrdersAdapter.OrderRow(
                                id = d.id,
                                status = status,
                                createdByUid = createdByUid,
                                itemCount = rawItems.size,
                                createdAtMs = createdAt
                            )
                        }.sortedByDescending { it.createdAtMs }

                        adapter.submit(items)
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "No autorizado", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    override fun onStop() {
        ordersListener?.remove()
        ordersListener = null
        super.onStop()
    }
}
