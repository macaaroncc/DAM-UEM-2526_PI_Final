package com.example.pi2dam

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.example.pi2dam.model.Product
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    data class OrderSummaryRow(
        val id: String,
        val createdAt: Date,
        val status: String,
        val lineCount: Int,
        val qtyTotal: Long,
        val amountTotal: Double
    )

    fun stockReport(
        context: Context,
        title: String,
        generatedAt: Date,
        products: List<Product>
    ): ByteArray {
        val doc = PdfDocument()
        val out = ByteArrayOutputStream()

        val pageW = 595
        val pageH = 842
        val margin = 36

        val accent = getColor(context, R.color.brand_orange)
        val textPrimary = getColor(context, R.color.text_primary)
        val textSecondary = getColor(context, R.color.text_secondary)
        val divider = getColor(context, R.color.divider)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 11f
        }
        val cellHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 11f
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            textSize = 10.5f
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = divider
            strokeWidth = 1f
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            strokeWidth = 3f
        }

        val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val moneyFmt = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("es").setRegion("ES").build())

        val rows = products.sortedBy { it.name.lowercase(Locale.getDefault()) }

        // Table columns (x positions)
        val colName = margin
        val colSku = 295
        val colLoc = 370
        val colStock = 470
        val colPrice = 525

        var pageNum = 1
        var y = 0

        fun newPage(): PdfDocument.Page {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
            val c = page.canvas
            y = margin

            // Header
            c.drawText(context.getString(R.string.brand_name), margin.toFloat(), y.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 16f
            })
            y += 22
            c.drawText(title, margin.toFloat(), y.toFloat(), headerPaint)
            y += 18
            c.drawText("Generado: ${dateFmt.format(generatedAt)}", margin.toFloat(), y.toFloat(), subPaint)
            y += 10
            c.drawLine(margin.toFloat(), (y + 6).toFloat(), (pageW - margin).toFloat(), (y + 6).toFloat(), accentPaint)
            y += 22

            // Table header
            c.drawText("Producto", colName.toFloat(), y.toFloat(), cellHeaderPaint)
            c.drawText("SKU", colSku.toFloat(), y.toFloat(), cellHeaderPaint)
            c.drawText("Ubicación", colLoc.toFloat(), y.toFloat(), cellHeaderPaint)
            c.drawText("Stock", colStock.toFloat(), y.toFloat(), cellHeaderPaint)
            c.drawText("€", colPrice.toFloat(), y.toFloat(), cellHeaderPaint)
            y += 10
            c.drawLine(margin.toFloat(), y.toFloat(), (pageW - margin).toFloat(), y.toFloat(), linePaint)
            y += 14

            return page
        }

        var page = newPage()
        var canvas = page.canvas

        rows.forEach { p ->
            // Simple pagination
            if (y > pageH - margin - 40) {
                // footer
                drawFooter(canvas, pageW, pageH, margin, pageNum)
                doc.finishPage(page)
                pageNum++
                page = newPage()
                canvas = page.canvas
            }

            // Name (wrapped)
            val nameMaxW = (colSku - 8 - colName)
            val nameLines = wrap(p.name, cellPaint, nameMaxW)
            val baseLineY = y
            nameLines.take(2).forEachIndexed { i, line ->
                canvas.drawText(line, colName.toFloat(), (baseLineY + i * 12).toFloat(), cellPaint)
            }

            canvas.drawText(p.sku, colSku.toFloat(), y.toFloat(), cellPaint)
            canvas.drawText(p.location, colLoc.toFloat(), y.toFloat(), cellPaint)
            canvas.drawText(p.stock.toString(), colStock.toFloat(), y.toFloat(), cellPaint)
            canvas.drawText(moneyFmt.format(p.price).replace("€", "").trim(), colPrice.toFloat(), y.toFloat(), cellPaint)

            val rowH = maxOf(14, nameLines.size * 12)
            y += rowH
            canvas.drawLine(margin.toFloat(), (y - 6).toFloat(), (pageW - margin).toFloat(), (y - 6).toFloat(), linePaint)
        }

        drawFooter(canvas, pageW, pageH, margin, pageNum)
        doc.finishPage(page)

        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    fun monthlyOrdersReport(
        context: Context,
        title: String,
        periodLabel: String,
        generatedAt: Date,
        orders: List<OrderSummaryRow>
    ): ByteArray {
        val doc = PdfDocument()
        val out = ByteArrayOutputStream()

        val pageW = 595
        val pageH = 842
        val margin = 36

        val accent = getColor(context, R.color.brand_orange)
        val textPrimary = getColor(context, R.color.text_primary)
        val textSecondary = getColor(context, R.color.text_secondary)
        val divider = getColor(context, R.color.divider)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 11f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            textSize = 11f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 13f
        }
        val cellHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 11f
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            textSize = 10.5f
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = divider
            strokeWidth = 1f
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            strokeWidth = 3f
        }

        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dtFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val moneyFmt = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("es").setRegion("ES").build())

        val rows = orders.sortedByDescending { it.createdAt.time }

        val totalOrders = rows.size
        val cancelled = rows.count { it.status.equals("CANCELLED", ignoreCase = true) }
        val qtyTotal = rows.sumOf { it.qtyTotal }
        val amountTotal = rows.sumOf { it.amountTotal }

        // Table columns
        val colDate = margin
        val colId = 125
        val colStatus = 315
        val colLines = 395
        val colTotal = 455

        var pageNum = 1
        var y = margin

        fun newPage(): PdfDocument.Page {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
            val c = page.canvas
            y = margin

            c.drawText(context.getString(R.string.brand_name), margin.toFloat(), y.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 16f
            })
            y += 22
            c.drawText(title, margin.toFloat(), y.toFloat(), titlePaint)
            y += 18
            c.drawText("Periodo: $periodLabel", margin.toFloat(), y.toFloat(), subPaint)
            y += 14
            c.drawText("Generado: ${dtFmt.format(generatedAt)}", margin.toFloat(), y.toFloat(), subPaint)
            y += 10
            c.drawLine(margin.toFloat(), (y + 6).toFloat(), (pageW - margin).toFloat(), (y + 6).toFloat(), accentPaint)
            y += 18

            // Summary chips (simple blocks)
            var x = margin
            fun summaryBlock(label: String, value: String) {
                c.drawText(label, x.toFloat(), y.toFloat(), labelPaint)
                c.drawText(value, x.toFloat(), (y + 16).toFloat(), valuePaint)
                x += 135
            }
            summaryBlock("Pedidos", totalOrders.toString())
            summaryBlock("Cancelados", cancelled.toString())
            summaryBlock("Unidades", qtyTotal.toString())
            summaryBlock("Total", moneyFmt.format(amountTotal))

            y += 40

            // Table header
            c.drawText("Fecha", colDate.toFloat(), y.toFloat(), cellHeaderPaint)
            c.drawText("ID", colId.toFloat(), y.toFloat(), cellHeaderPaint)
            c.drawText("Estado", colStatus.toFloat(), y.toFloat(), cellHeaderPaint)
            c.drawText("Líneas", colLines.toFloat(), y.toFloat(), cellHeaderPaint)
            c.drawText("Total", colTotal.toFloat(), y.toFloat(), cellHeaderPaint)
            y += 10
            c.drawLine(margin.toFloat(), y.toFloat(), (pageW - margin).toFloat(), y.toFloat(), linePaint)
            y += 14

            return page
        }

        var page = newPage()
        var canvas = page.canvas

        rows.forEach { o ->
            if (y > pageH - margin - 40) {
                drawFooter(canvas, pageW, pageH, margin, pageNum)
                doc.finishPage(page)
                pageNum++
                page = newPage()
                canvas = page.canvas
            }

            canvas.drawText(dateFmt.format(o.createdAt), colDate.toFloat(), y.toFloat(), cellPaint)
            canvas.drawText(o.id.take(12), colId.toFloat(), y.toFloat(), cellPaint)
            canvas.drawText(o.status, colStatus.toFloat(), y.toFloat(), cellPaint)
            canvas.drawText(o.lineCount.toString(), colLines.toFloat(), y.toFloat(), cellPaint)
            canvas.drawText(moneyFmt.format(o.amountTotal), colTotal.toFloat(), y.toFloat(), cellPaint)

            y += 14
            canvas.drawLine(margin.toFloat(), (y - 6).toFloat(), (pageW - margin).toFloat(), (y - 6).toFloat(), linePaint)
        }

        drawFooter(canvas, pageW, pageH, margin, pageNum)
        doc.finishPage(page)

        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Int): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        val lines = ArrayList<String>()
        var current = words[0]
        for (i in 1 until words.size) {
            val test = "$current ${words[i]}"
            if (paint.measureText(test) <= maxWidth) {
                current = test
            } else {
                lines.add(current)
                current = words[i]
            }
        }
        lines.add(current)
        return lines
    }

    private fun drawFooter(canvas: Canvas, pageW: Int, pageH: Int, margin: Int, pageNum: Int) {
        val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 10f
        }
        canvas.drawText("Página $pageNum", (pageW - margin - 60).toFloat(), (pageH - margin + 10).toFloat(), footer)
    }

    @ColorInt
    private fun getColor(context: Context, id: Int): Int {
        return ContextCompat.getColor(context, id)
    }
}
