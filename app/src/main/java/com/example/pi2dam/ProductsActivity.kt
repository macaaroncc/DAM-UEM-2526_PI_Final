package com.example.pi2dam

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.model.Product
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.material.textfield.TextInputLayout

class ProductsActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_INITIAL_QUERY = "extra_initial_query"
    }
    private enum class Filter { ALL, LOW_STOCK }
    private enum class SupplierFilter { ALL, WITH_SUPPLIER, WITHOUT_SUPPLIER }
    private enum class Sort { NAME, PRICE_ASC, PRICE_DESC }

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
    private var suppliersListener: com.google.firebase.firestore.ListenerRegistration? = null

    private var allProducts: List<Product> = emptyList()
    private var suppliersById: Map<String, String> = emptyMap()
    private var currentQuery = ""
    private var currentFilter = Filter.ALL
    private var currentSupplierFilter = SupplierFilter.ALL
    private var currentSort = Sort.NAME

    private lateinit var tvProductsSummary: TextView
    private lateinit var tvProductsEmpty: TextView

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
        tvProductsSummary = findViewById(R.id.tvProductsSummary)
        tvProductsEmpty = findViewById(R.id.tvProductsEmpty)

        setupSearchAndFilters()
        val initialQuery = intent.getStringExtra(EXTRA_INITIAL_QUERY).orEmpty().trim()
        if (initialQuery.isNotBlank()) {
            currentQuery = initialQuery
            findViewById<TextInputLayout>(R.id.tilProductsSearch).editText?.setText(initialQuery)
        }

        findViewById<MaterialButton>(R.id.btnExportStockPdf).setOnClickListener {
            exportStockPdf()
        }

        findViewById<RecyclerView>(R.id.rvProducts).apply {
            layoutManager = LinearLayoutManager(this@ProductsActivity)
            adapter = this@ProductsActivity.adapter
        }

        applyFilters()
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
                        allProducts = snap.documents.map { d ->
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
                        }

                        applyFilters()
                    }

                suppliersListener?.remove()
                suppliersListener = FirebaseRefs.db.collection(FirebaseRefs.COL_SUPPLIERS)
                    .addSnapshotListener { snap, e ->
                        if (e != null || snap == null) return@addSnapshotListener
                        suppliersById = snap.documents.associate { d ->
                            d.id to (d.getString("name") ?: "")
                        }
                        applyFilters()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "No autorizado", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
    private fun setupSearchAndFilters() {
        val til = findViewById<TextInputLayout>(R.id.tilProductsSearch)
        til.editText?.doAfterTextChanged {
            currentQuery = it?.toString().orEmpty()
            applyFilters()
        }

        findViewById<View>(R.id.chipProductsFilterAll).setOnClickListener {
            currentFilter = Filter.ALL
            applyFilters()
        }
        findViewById<View>(R.id.chipProductsFilterLowStock).setOnClickListener {
            currentFilter = Filter.LOW_STOCK
            applyFilters()
        }
        findViewById<View>(R.id.chipProductsSupplierAll).setOnClickListener {
            currentSupplierFilter = SupplierFilter.ALL
            applyFilters()
        }
        findViewById<View>(R.id.chipProductsSupplierLinked).setOnClickListener {
            currentSupplierFilter = SupplierFilter.WITH_SUPPLIER
            applyFilters()
        }
        findViewById<View>(R.id.chipProductsSupplierUnlinked).setOnClickListener {
            currentSupplierFilter = SupplierFilter.WITHOUT_SUPPLIER
            applyFilters()
        }
        findViewById<View>(R.id.chipProductsSortName).setOnClickListener {
            currentSort = Sort.NAME
            applyFilters()
        }
        findViewById<View>(R.id.chipProductsSortPriceAsc).setOnClickListener {
            currentSort = Sort.PRICE_ASC
            applyFilters()
        }
        findViewById<View>(R.id.chipProductsSortPriceDesc).setOnClickListener {
            currentSort = Sort.PRICE_DESC
            applyFilters()
        }
    }

    private fun applyFilters() {
        val q = currentQuery.trim().lowercase()
        var visible = allProducts

        if (q.isNotBlank()) {
            visible = visible.filter { p ->
                val supplierName = suppliersById[p.supplierId].orEmpty()
                p.name.lowercase().contains(q) ||
                    p.sku.lowercase().contains(q) ||
                    p.location.lowercase().contains(q) ||
                    supplierName.lowercase().contains(q)
            }
        }

        visible = when (currentFilter) {
            Filter.ALL -> visible
            Filter.LOW_STOCK -> visible.filter { it.stock <= it.lowStockThreshold }
        }
        visible = when (currentSupplierFilter) {
            SupplierFilter.ALL -> visible
            SupplierFilter.WITH_SUPPLIER -> visible.filter { it.supplierId.isNotBlank() }
            SupplierFilter.WITHOUT_SUPPLIER -> visible.filter { it.supplierId.isBlank() }
        }

        visible = when (currentSort) {
            Sort.NAME -> visible.sortedBy { it.name.lowercase() }
            Sort.PRICE_ASC -> visible.sortedBy { it.price }
            Sort.PRICE_DESC -> visible.sortedByDescending { it.price }
        }

        adapter.submit(visible, suppliersById)

        val lowStockCount = allProducts.count { it.stock <= it.lowStockThreshold }
        val noSupplierCount = allProducts.count { it.supplierId.isBlank() }
        tvProductsSummary.text = getString(
            R.string.products_summary,
            visible.size,
            allProducts.size,
            lowStockCount,
            noSupplierCount
        )
        tvProductsEmpty.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
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
        suppliersListener?.remove()
        suppliersListener = null
        super.onStop()
    }
}
