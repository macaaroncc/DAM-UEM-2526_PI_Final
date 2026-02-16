package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.adapter.ProductAdapter
import com.example.pi2dam.model.Product
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class HomeActivity : AppCompatActivity() {

    private lateinit var adapter: ProductAdapter
    private var allProducts = listOf<Product>()
    private var currentFilter: Filter = Filter.NONE
    private var searchQuery = ""

    private enum class Filter { NONE, LOW_STOCK, HIGH_PRICE, LOW_PRICE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        AppMenu.bind(this)

        findViewById<View>(R.id.btnHelp).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        setupRecyclerView()
        setupSearch()
        setupFilters()
        loadProducts()
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter { product ->
            startActivity(Intent(this, ProductActivity::class.java)
                .putExtra(ProductActivity.EXTRA_PRODUCT_NAME, product.name))
        }
        findViewById<RecyclerView>(R.id.rvProducts).apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = this@HomeActivity.adapter
        }
    }

    private fun setupSearch() {
        val til = findViewById<TextInputLayout>(R.id.tilSearchHome)
        val et = til.editText as? TextInputEditText
        et?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString().orEmpty()
                applyFilters()
            }
        })
        til.setEndIconOnClickListener { applyFilters() }
    }

    private fun setupFilters() {
        val btnRecent = findViewById<MaterialButton>(R.id.btnFilterRecent)
        val btnLowStock = findViewById<MaterialButton>(R.id.btnFilterLowStock)
        val btnHighPrice = findViewById<MaterialButton>(R.id.btnFilterHighPrice)
        val btnLowPrice = findViewById<MaterialButton>(R.id.btnFilterLowPrice)

        btnRecent.setOnClickListener { setFilter(Filter.NONE) }
        btnLowStock.setOnClickListener { setFilter(Filter.LOW_STOCK) }
        btnHighPrice.setOnClickListener { setFilter(Filter.HIGH_PRICE) }
        btnLowPrice.setOnClickListener { setFilter(Filter.LOW_PRICE) }
    }

    private fun setFilter(filter: Filter) {
        currentFilter = filter
        applyFilters()
    }

    private fun applyFilters() {
        var list = allProducts

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.sku.lowercase().contains(q) ||
                it.location.lowercase().contains(q)
            }
        }

        list = when (currentFilter) {
            Filter.NONE -> list
            Filter.LOW_STOCK -> list.filter { it.stock <= it.lowStockThreshold }
            Filter.HIGH_PRICE -> list.sortedByDescending { it.price }
            Filter.LOW_PRICE -> list.sortedBy { it.price }
        }

        adapter.submitList(list)
    }

    private fun loadProducts() {
        FirebaseRefs.db.collection(FirebaseRefs.COL_PRODUCTS)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                allProducts = snap.documents.mapNotNull { doc ->
                    Product(
                        id = doc.id,
                        name = doc.getString("name").orEmpty(),
                        sku = doc.getString("sku").orEmpty(),
                        location = doc.getString("location").orEmpty(),
                        stock = doc.getLong("stock") ?: 0,
                        price = doc.getDouble("price") ?: 0.0,
                        lowStockThreshold = doc.getLong("lowStockThreshold") ?: 0
                    )
                }
                applyFilters()
            }
    }
}
