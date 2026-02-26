package com.example.pi2dam

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.model.OrderItem
import com.example.pi2dam.model.Product
import com.google.android.material.button.MaterialButton

class CreateOrderActivity : AppCompatActivity() {

    private val adapter = PickProductsAdapter()
    private var products: List<Product> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_order)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        AppMenu.bind(this)

        findViewById<RecyclerView>(R.id.rvPickProducts).apply {
            layoutManager = LinearLayoutManager(this@CreateOrderActivity)
            adapter = this@CreateOrderActivity.adapter
        }

        findViewById<MaterialButton>(R.id.btnSubmitOrder).setOnClickListener {
            val uid = FirebaseRefs.auth.currentUser?.uid
            if (uid.isNullOrBlank()) return@setOnClickListener

            val items = products.mapNotNull { p ->
                val qty = adapter.getQty(p.id)
                if (qty <= 0) null else OrderItem(productId = p.id, qty = qty, priceSnapshot = p.price)
            }

            PiRepository.createOrder(uid, items)
                .addOnSuccessListener { orderId ->
                    Toast.makeText(this, "Pedido creado: ${orderId.take(8)}", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "No se pudo crear", Toast.LENGTH_SHORT).show()
                }
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

                FirebaseRefs.db.collection(FirebaseRefs.COL_PRODUCTS).get()
                    .addOnSuccessListener { snap ->
                        products = snap.documents.map { d ->
                            Product(
                                id = d.id,
                                name = d.getString("name") ?: "",
                                sku = d.getString("sku") ?: "",
                                location = d.getString("location") ?: "",
                                supplierId = d.getString("supplierId") ?: "",
                                stock = d.getLong("stock") ?: 0L,
                                price = d.getDouble("price") ?: 0.0,
                                lowStockThreshold = d.getLong("lowStockThreshold") ?: 0L
                            )
                        }.filter { it.stock > 0 }.sortedBy { it.name }

                        adapter.submit(products)
                    }
            }
            .addOnFailureListener { finish() }
    }
}
