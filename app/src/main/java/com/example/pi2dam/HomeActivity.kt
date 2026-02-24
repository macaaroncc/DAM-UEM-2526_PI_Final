package com.example.pi2dam

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pi2dam.model.EmployeeProfile
import com.example.pi2dam.model.EmployeeProfile.Companion.ROLE_ADMIN
import com.example.pi2dam.model.Product
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private val topSoldAdapter = TopProductsAdapter()

    private var productsReg: ListenerRegistration? = null

    private lateinit var tvGreeting: TextView
    private lateinit var tvGreetingSub: TextView

    private lateinit var tvProductsCount: TextView
    private lateinit var tvUnitsTotal: TextView
    private lateinit var tvLowStockCount: TextView
    private lateinit var tvInventoryValue: TextView

    private lateinit var tvMonthInvestment: TextView
    private lateinit var tvMonthUnits: TextView
    private lateinit var tvMonthOrders: TextView
    private lateinit var tvEmptyTopSold: TextView

    private val esES = Locale.Builder().setLanguage("es").setRegion("ES").build()
    private val money = NumberFormat.getCurrencyInstance(esES)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        AppMenu.bind(this)

        bindViews()

        findViewById<View>(R.id.btnHelp).setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        setupTopSoldRecyclerView()
    }

    override fun onStart() {
        super.onStart()

        val current = FirebaseRefs.auth.currentUser
        if (current == null) {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        val cached = Session.employee
        if (cached != null && cached.uid == current.uid) {
            updateGreeting(cached)
            subscribeProducts()
            loadMonthStats()
            return
        }

        PiRepository.ensureEmployeeAccess(current.uid)
            .addOnSuccessListener {
                Session.setEmployee(it)
                updateGreeting(it)
                subscribeProducts()
                loadMonthStats()
            }
            .addOnFailureListener {
                FirebaseRefs.auth.signOut()
                Session.clear()
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
    }

    override fun onStop() {
        productsReg?.remove()
        productsReg = null
        super.onStop()
    }

    private fun bindViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvGreetingSub = findViewById(R.id.tvGreetingSub)

        tvProductsCount = findViewById(R.id.tvProductsCount)
        tvUnitsTotal = findViewById(R.id.tvUnitsTotal)
        tvLowStockCount = findViewById(R.id.tvLowStockCount)
        tvInventoryValue = findViewById(R.id.tvInventoryValue)

        tvMonthInvestment = findViewById(R.id.tvMonthInvestment)
        tvMonthUnits = findViewById(R.id.tvMonthUnits)
        tvMonthOrders = findViewById(R.id.tvMonthOrders)
        tvEmptyTopSold = findViewById(R.id.tvEmptyTopSold)

    }

    private fun updateGreeting(profile: EmployeeProfile) {
        val name = profile.name.ifBlank { profile.email.ifBlank { "" } }
        tvGreeting.text = if (name.isBlank()) getString(R.string.home_greeting_fallback)
        else getString(R.string.home_greeting, name)

        val roleLabel = if (profile.role == ROLE_ADMIN) getString(R.string.home_role_admin)
        else getString(R.string.home_role_worker)

        val today = SimpleDateFormat("d MMM yyyy", esES).format(Calendar.getInstance().time)
        tvGreetingSub.text = "$roleLabel · $today"

    }

    private fun setupTopSoldRecyclerView() {
        findViewById<RecyclerView>(R.id.rvTopSoldProducts).apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = topSoldAdapter
        }
    }

    private fun updateInventorySummary(products: List<Product>) {
        val productsCount = products.size
        val unitsTotal = products.fold(0L) { acc, p -> acc + p.stock }
        val lowStockCount = products.count { it.stock <= it.lowStockThreshold }
        val inventoryValue = products.fold(0.0) { acc, p -> acc + (p.stock.toDouble() * p.price) }

        tvProductsCount.text = productsCount.toString()
        tvUnitsTotal.text = unitsTotal.toString()
        tvLowStockCount.text = lowStockCount.toString()
        tvInventoryValue.text = money.format(inventoryValue)
    }

    private fun subscribeProducts() {
        productsReg?.remove()
        productsReg = FirebaseRefs.db.collection(FirebaseRefs.COL_PRODUCTS)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener

                val products = snap.documents.mapNotNull { doc ->
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

                updateInventorySummary(products)
            }
    }

    private fun loadMonthStats() {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)

        val start = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val end = Calendar.getInstance().apply {
            time = start
            add(Calendar.MONTH, 1)
        }.time

        val monthLabel = SimpleDateFormat("MMMM yyyy", esES).format(start)

        FirebaseRefs.db.collection(FirebaseRefs.COL_ORDERS)
            .whereGreaterThanOrEqualTo("createdAt", Timestamp(start))
            .whereLessThan("createdAt", Timestamp(end))
            .get()
            .addOnSuccessListener { ordersSnap ->
                val docs = ordersSnap.documents
                val cancelled = docs.count { (it.getString("status") ?: "") == "CANCELLED" }

                val qtyByProduct = HashMap<String, Long>()
                var unitsTotal = 0L
                var amountTotal = 0.0

                docs.forEach { d ->
                    @Suppress("UNCHECKED_CAST")
                    val rawItems = (d.get("items") as? List<Map<String, Any?>>).orEmpty()
                    rawItems.forEach { m ->
                        val pid = m["productId"] as? String ?: return@forEach
                        val qty = (m["qty"] as? Number)?.toLong() ?: 0L
                        val price = (m["priceSnapshot"] as? Number)?.toDouble() ?: 0.0
                        if (qty <= 0) return@forEach

                        unitsTotal += qty
                        amountTotal += qty * price
                        qtyByProduct[pid] = (qtyByProduct[pid] ?: 0L) + qty
                    }
                }

                tvMonthInvestment.text = money.format(amountTotal)
                tvMonthUnits.text = unitsTotal.toString()
                tvMonthOrders.text = "${getString(R.string.dashboard_orders_count, docs.size, cancelled)} · $monthLabel"

                if (qtyByProduct.isEmpty()) {
                    topSoldAdapter.submit(emptyList())
                    tvEmptyTopSold.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                FirebaseRefs.db.collection(FirebaseRefs.COL_PRODUCTS)
                    .get()
                    .addOnSuccessListener { productsSnap ->
                        val nameById = productsSnap.documents.associate { it.id to (it.getString("name") ?: it.id) }

                        val top = qtyByProduct.entries
                            .sortedByDescending { it.value }
                            .take(5)

                        val max = top.firstOrNull()?.value ?: 1L
                        val rows = top.map { e ->
                            val pct = ((e.value.toDouble() / max.toDouble()) * 100.0).toInt()
                            TopProductsAdapter.Row(
                                name = nameById[e.key] ?: e.key,
                                qty = e.value,
                                pct = pct
                            )
                        }

                        topSoldAdapter.submit(rows)
                        tvEmptyTopSold.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                    }
                    .addOnFailureListener {
                        val top = qtyByProduct.entries
                            .sortedByDescending { it.value }
                            .take(5)
                        val max = top.firstOrNull()?.value ?: 1L
                        topSoldAdapter.submit(top.map { e ->
                            TopProductsAdapter.Row(
                                name = e.key,
                                qty = e.value,
                                pct = ((e.value.toDouble() / max.toDouble()) * 100.0).toInt()
                            )
                        })
                        tvEmptyTopSold.visibility = View.GONE
                    }
            }
            .addOnFailureListener {
                tvMonthInvestment.text = "--"
                tvMonthUnits.text = "--"
                tvMonthOrders.text = "--"
                topSoldAdapter.submit(emptyList())
                tvEmptyTopSold.visibility = View.VISIBLE
            }
    }
}
