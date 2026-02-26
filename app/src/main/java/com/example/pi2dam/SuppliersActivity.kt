package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.model.Supplier
import com.google.android.material.button.MaterialButton

class SuppliersActivity : AppCompatActivity() {

    private var suppliersListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val adapter = SuppliersAdapter(
        onClick = { supplier ->
            startActivity(Intent(this, SupplierFormActivity::class.java)
                .putExtra(SupplierFormActivity.EXTRA_SUPPLIER_ID, supplier.id))
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_suppliers)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        AppMenu.bind(this)

        findViewById<MaterialButton>(R.id.btnCreateSupplier).setOnClickListener {
            startActivity(Intent(this, SupplierFormActivity::class.java))
        }

        findViewById<RecyclerView>(R.id.rvSuppliers).apply {
            layoutManager = LinearLayoutManager(this@SuppliersActivity)
            adapter = this@SuppliersActivity.adapter
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

                suppliersListener?.remove()
                suppliersListener = FirebaseRefs.db.collection(FirebaseRefs.COL_SUPPLIERS)
                    .addSnapshotListener { snap, e ->
                        if (e != null || snap == null) return@addSnapshotListener

                        val items = snap.documents.map { d ->
                            Supplier(
                                id = d.id,
                                name = d.getString("name") ?: ""
                            )
                        }.sortedBy { it.name.lowercase() }

                        adapter.submit(items)
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "No autorizado", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    override fun onStop() {
        suppliersListener?.remove()
        suppliersListener = null
        super.onStop()
    }
}
