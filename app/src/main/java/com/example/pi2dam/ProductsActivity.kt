package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.model.Product
import com.google.android.material.button.MaterialButton

class ProductsActivity : AppCompatActivity() {

    private var productsListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val adapter = ProductsAdapter(
        onClick = { p ->
            startActivity(Intent(this, ProductFormActivity::class.java)
                .putExtra(ProductFormActivity.EXTRA_PRODUCT_ID, p.id))
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_products)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        AppMenu.bind(this)

        findViewById<MaterialButton>(R.id.btnCreateProduct).setOnClickListener {
            startActivity(Intent(this, ProductFormActivity::class.java))
        }

        findViewById<RecyclerView>(R.id.rvProducts).apply {
            layoutManager = LinearLayoutManager(this@ProductsActivity)
            adapter = this@ProductsActivity.adapter
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

                productsListener?.remove()
                productsListener = FirebaseRefs.db.collection(FirebaseRefs.COL_PRODUCTS)
                    .addSnapshotListener { snap, e ->
                        if (e != null || snap == null) return@addSnapshotListener

                        val items = snap.documents.map { d ->
                            Product(
                                id = d.id,
                                name = d.getString("name") ?: "",
                                sku = d.getString("sku") ?: "",
                                location = d.getString("location") ?: "",
                                stock = d.getLong("stock") ?: 0L,
                                price = d.getDouble("price") ?: 0.0,
                                lowStockThreshold = d.getLong("lowStockThreshold") ?: 0L
                            )
                        }.sortedBy { it.name }

                        adapter.submit(items)
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "No autorizado", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    override fun onStop() {
        productsListener?.remove()
        productsListener = null
        super.onStop()
    }
}
