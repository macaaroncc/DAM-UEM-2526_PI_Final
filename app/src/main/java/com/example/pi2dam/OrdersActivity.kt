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
import com.example.pi2dam.model.EmployeeProfile.Companion.ROLE_ADMIN
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class OrdersActivity : AppCompatActivity() {

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
                // no-op (viewer might be missing)
            }
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "No se pudo guardar el PDF", Toast.LENGTH_SHORT).show()
        } finally {
            pendingPdf = null
            pendingPdfName = null
        }
    }

    private var ordersListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val adapter = OrdersAdapter(
        onCancel = { orderId ->
            PiRepository.cancelOrder(orderId)
                .addOnSuccessListener {
                    Toast.makeText(this, "Pedido cancelado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "No se pudo cancelar", Toast.LENGTH_SHORT).show()
                }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_orders)

        findViewById<View>(R.id.main).applySystemBarsPadding()

        AppMenu.bind(this)

        findViewById<MaterialButton>(R.id.btnCreateOrder).setOnClickListener {
            startActivity(Intent(this, CreateOrderActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnExportOrdersPdf).setOnClickListener {
            exportMonthlyOrdersPdf()
        }

        findViewById<RecyclerView>(R.id.rvOrders).apply {
            layoutManager = LinearLayoutManager(this@OrdersActivity)
            adapter = this@OrdersActivity.adapter
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
                adapter.setPermissions(currentUid = current.uid, isAdmin = me.role == ROLE_ADMIN)

                ordersListener?.remove()
                ordersListener = FirebaseRefs.db.collection(FirebaseRefs.COL_ORDERS)
                    .addSnapshotListener { snap, e ->
                        if (e != null || snap == null) return@addSnapshotListener

                        val items = snap.documents.map { d ->
                            val status = d.getString("status") ?: ""
                            val createdByUid = d.getString("createdByUid") ?: ""
                            val createdAt = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L

                            @Suppress("UNCHECKED_CAST")
                            val rawItems = (d.get("items") as? List<Map<String, Any?>>).orEmpty()

                            OrdersAdapter.OrderRow(
                                id = d.id,
                                status = status,
                                createdByUid = createdByUid,
                                itemCount = rawItems.size,
                                createdAtMs = createdAt
                            )
                        }.sortedByDescending { it.createdAtMs }

                        adapter.submit(items)
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "No autorizado", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun exportMonthlyOrdersPdf() {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) // 0-based

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

        val fileMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(start)
        val title = "Resumen de pedidos"
        val esES = Locale.Builder().setLanguage("es").setRegion("ES").build()
        val periodLabel = SimpleDateFormat("MMMM yyyy", esES).format(start)

        FirebaseRefs.db.collection(FirebaseRefs.COL_ORDERS)
            .whereGreaterThanOrEqualTo("createdAt", Timestamp(start))
            .whereLessThan("createdAt", Timestamp(end))
            .get()
            .addOnSuccessListener { snap ->
                val rows = snap.documents.map { d ->
                    val status = d.getString("status") ?: ""
                    val createdAt = d.getTimestamp("createdAt")?.toDate() ?: Date(0)

                    @Suppress("UNCHECKED_CAST")
                    val rawItems = (d.get("items") as? List<Map<String, Any?>>).orEmpty()
                    val lineCount = rawItems.size
                    val qtyTotal = rawItems.sumOf { (it["qty"] as? Number)?.toLong() ?: 0L }
                    val amountTotal = rawItems.sumOf {
                        val qty = (it["qty"] as? Number)?.toLong() ?: 0L
                        val price = (it["priceSnapshot"] as? Number)?.toDouble() ?: 0.0
                        qty * price
                    }

                    PdfReportGenerator.OrderSummaryRow(
                        id = d.id,
                        createdAt = createdAt,
                        status = status,
                        lineCount = lineCount,
                        qtyTotal = qtyTotal,
                        amountTotal = amountTotal
                    )
                }

                val bytes = PdfReportGenerator.monthlyOrdersReport(
                    context = this,
                    title = title,
                    periodLabel = periodLabel,
                    generatedAt = Date(),
                    orders = rows
                )

                pendingPdf = bytes
                pendingPdfName = "pedidos_$fileMonth.pdf"
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
        ordersListener?.remove()
        ordersListener = null
        super.onStop()
    }
}
