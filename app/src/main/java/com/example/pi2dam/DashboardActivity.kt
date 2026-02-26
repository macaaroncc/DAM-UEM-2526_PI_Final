package com.example.pi2dam

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private val adapter = TopProductsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)

        findViewById<View>(R.id.main).applySystemBarsPadding()
        AppMenu.bind(this)

        findViewById<RecyclerView>(R.id.rvTopProducts).apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = this@DashboardActivity.adapter
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
            .addOnSuccessListener {
                Session.setEmployee(it)
                loadStats()
            }
            .addOnFailureListener {
                Toast.makeText(this, "No autorizado", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadStats() {
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

        val esES = Locale.Builder().setLanguage("es").setRegion("ES").build()
        val money = NumberFormat.getCurrencyInstance(esES)
        val monthLabel = SimpleDateFormat("MMMM yyyy", esES).format(start)

        val tvTotalInvestment = findViewById<TextView>(R.id.tvTotalInvestment)
        val tvTotalUnits = findViewById<TextView>(R.id.tvTotalUnits)
        val tvOrdersCount = findViewById<TextView>(R.id.tvOrdersCount)
        val tvEmptyTop = findViewById<TextView>(R.id.tvEmptyTop)

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

                tvTotalInvestment.text = money.format(amountTotal)
                tvTotalUnits.text = unitsTotal.toString()
                tvOrdersCount.text = "${getString(R.string.dashboard_orders_count, docs.size, cancelled)} · $monthLabel"

                if (qtyByProduct.isEmpty()) {
                    adapter.submit(emptyList())
                    tvEmptyTop.visibility = View.VISIBLE
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

                        adapter.submit(rows)
                        tvEmptyTop.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                    }
                    .addOnFailureListener {
                        // fallback (show ids)
                        val top = qtyByProduct.entries.sortedByDescending { it.value }.take(5)
                        val max = top.firstOrNull()?.value ?: 1L
                        adapter.submit(top.map { e ->
                            TopProductsAdapter.Row(
                                name = e.key,
                                qty = e.value,
                                pct = ((e.value.toDouble() / max.toDouble()) * 100.0).toInt()
                            )
                        })
                        tvEmptyTop.visibility = View.GONE
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, e.message ?: "No se pudieron cargar estadísticas", Toast.LENGTH_SHORT).show()
            }
    }
}
