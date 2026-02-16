package com.example.pi2dam

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.pi2dam.model.Product
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ProductFormActivity : AppCompatActivity() {

    private var editingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_product_form)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        editingId = intent.getStringExtra(EXTRA_PRODUCT_ID)

        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etSku = findViewById<TextInputEditText>(R.id.etSku)
        val etLocation = findViewById<TextInputEditText>(R.id.etLocation)
        val etStock = findViewById<TextInputEditText>(R.id.etStock)
        val etPrice = findViewById<TextInputEditText>(R.id.etPrice)
        val etLow = findViewById<TextInputEditText>(R.id.etLowStock)

        val isEdit = !editingId.isNullOrBlank()
        findViewById<MaterialButton>(R.id.btnDelete).visibility = if (isEdit) View.VISIBLE else View.GONE

        AppMenu.bind(this)

        if (isEdit) {
            val id = editingId!!
            FirebaseRefs.db.collection(FirebaseRefs.COL_PRODUCTS).document(id).get()
                .addOnSuccessListener { d ->
                    etName.setText(d.getString("name") ?: "")
                    etSku.setText(d.getString("sku") ?: "")
                    etLocation.setText(d.getString("location") ?: "")
                    etStock.setText((d.getLong("stock") ?: 0L).toString())
                    etPrice.setText((d.getDouble("price") ?: 0.0).toString())
                    etLow.setText((d.getLong("lowStockThreshold") ?: 0L).toString())
                }
        }

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val name = etName.text?.toString().orEmpty().trim()
            if (name.isBlank()) {
                Toast.makeText(this, "Nombre obligatorio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sku = etSku.text?.toString().orEmpty().trim()
            val location = etLocation.text?.toString().orEmpty().trim()
            val stock = etStock.text?.toString().orEmpty().trim().toLongOrNull() ?: 0L
            val price = etPrice.text?.toString().orEmpty().trim().toDoubleOrNull() ?: 0.0
            val low = etLow.text?.toString().orEmpty().trim().toLongOrNull() ?: 0L

            val p = Product(
                id = editingId.orEmpty(),
                name = name,
                sku = sku,
                location = location,
                stock = stock,
                price = price,
                lowStockThreshold = low
            )

            PiRepository.upsertProduct(p)
                .addOnSuccessListener {
                    Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "Error guardando", Toast.LENGTH_SHORT).show()
                }
        }

        findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener {
            val id = editingId ?: return@setOnClickListener
            PiRepository.deleteProduct(id)
                .addOnSuccessListener {
                    Toast.makeText(this, "Producto borrado", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "No se pudo borrar", Toast.LENGTH_SHORT).show()
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
            .addOnSuccessListener { Session.setEmployee(it) }
            .addOnFailureListener { finish() }
    }

    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"
    }
}
