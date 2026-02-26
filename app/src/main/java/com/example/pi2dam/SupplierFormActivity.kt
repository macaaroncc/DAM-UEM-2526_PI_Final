package com.example.pi2dam

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.pi2dam.model.Supplier
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SupplierFormActivity : AppCompatActivity() {

    private var editingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_supplier_form)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        editingId = intent.getStringExtra(EXTRA_SUPPLIER_ID)

        val etName = findViewById<TextInputEditText>(R.id.etSupplierName)
        val isEdit = !editingId.isNullOrBlank()
        findViewById<MaterialButton>(R.id.btnDelete).visibility = if (isEdit) View.VISIBLE else View.GONE

        AppMenu.bind(this)

        if (isEdit) {
            val id = editingId!!
            FirebaseRefs.db.collection(FirebaseRefs.COL_SUPPLIERS).document(id).get()
                .addOnSuccessListener { d ->
                    etName.setText(d.getString("name") ?: "")
                }
        }

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val name = etName.text?.toString().orEmpty().trim()
            if (name.isBlank()) {
                Toast.makeText(this, "Nombre obligatorio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            PiRepository.upsertSupplier(
                Supplier(
                    id = editingId.orEmpty(),
                    name = name
                )
            )
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
            PiRepository.deleteSupplier(id)
                .addOnSuccessListener {
                    Toast.makeText(this, "Proveedor borrado", Toast.LENGTH_SHORT).show()
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
        const val EXTRA_SUPPLIER_ID = "extra_supplier_id"
    }
}
