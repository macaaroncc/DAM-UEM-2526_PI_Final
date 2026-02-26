package com.example.pi2dam

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.model.Product
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductsActivity : AppCompatActivity() {

    private var pendingPdf: ByteArray? = null
    private var pendingPdfName: String? = null

    private val savePdfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val uri = res.data?.data ?: return@registerForActivityResult

        val bytes = pendingPdf ?: return@registerForActivityResult
        try {
            val os = contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("No se pudo abrir el destino")
            os.use {
                it.write(bytes)
                it.flush()
            }
            Toast.makeText(this, "PDF guardado", Toast.LENGTH_SHORT).show()

            // Try to open it
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, "application/pdf")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                )
            } catch (_: Exception) {
                // no-op
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "No se pudo guardar el PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pendingPdf = null
            pendingPdfName = null
        }
    }

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

        findViewById<MaterialButton>(R.id.btnExportStockPdf).setOnClickListener {
            exportStockPdf()
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
                                supplierId = d.getString("supplierId") ?: "",
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

    private fun exportStockPdf() {
        FirebaseRefs.db.collection(FirebaseRefs.COL_PRODUCTS)
            .get()
            .addOnSuccessListener { snap ->
                val products = snap.documents.map { d ->
                    Product(
                        id = d.id,
                        name = d.getString("name") ?: "",
                        sku = d.getString("sku") ?: "",
                        location = d.getString("location") ?: "",
                        stock = d.getLong("stock") ?: 0L,
                        price = d.getDouble("price") ?: 0.0,
                        lowStockThreshold = d.getLong("lowStockThreshold") ?: 0L
                    )
                }

                val fileDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val bytes = PdfReportGenerator.stockReport(
                    context = this,
                    title = "Stock actual",
                    generatedAt = Date(),
                    products = products
                )

                pendingPdf = bytes
                pendingPdfName = "stock_$fileDate.pdf"
                launchCreatePdf(pendingPdfName!!)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
            }
    }

    private fun launchCreatePdf(fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        savePdfLauncher.launch(intent)
    }

    override fun onStop() {
        productsListener?.remove()
        productsListener = null
        super.onStop()
    }
}
