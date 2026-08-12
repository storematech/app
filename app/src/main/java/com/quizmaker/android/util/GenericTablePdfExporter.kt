package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders an arbitrary headers+rows table as a paginated summary PDF — shared by every "Tools"
 * export button. Mirrors ResponsesPdfExporter.kt's layout but with a generic title/columns/rows
 * instead of one fixed data shape.
 */
object GenericTablePdfExporter {

    private const val PAGE_WIDTH = 595 // A4 portrait at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f
    private const val ROW_HEIGHT = 24f

    /** [columns] pairs a header label with its column width in points; widths should sum to roughly PAGE_WIDTH - 2*MARGIN. */
    fun export(
        context: Context,
        fileName: String,
        title: String,
        columns: List<Pair<String, Float>>,
        rows: List<List<String>>,
        branding: PdfBranding = PdfBranding.NONE
    ): Intent {
        val document = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 16f; isFakeBoldText = true }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 10f }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 9.5f; isFakeBoldText = true }
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6366F1") }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 9.5f }
        val altRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F5F5FB") }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 1f }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = PdfLetterhead.draw(canvas, MARGIN, PAGE_WIDTH - MARGIN, MARGIN, branding)

        fun drawHeaderRow() {
            var x = MARGIN
            canvas.drawRect(x, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, headerBgPaint)
            columns.forEach { (label, width) ->
                canvas.drawText(label, x + 4f, y + ROW_HEIGHT - 7f, headerTextPaint)
                x += width
            }
            y += ROW_HEIGHT
        }

        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
            drawHeaderRow()
        }

        canvas.drawText(title, MARGIN, y + 16f, titlePaint)
        y += 30f
        val generatedAt = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Total: ${rows.size}   Generated: $generatedAt", MARGIN, y, labelPaint)
        y += 18f

        drawHeaderRow()

        rows.forEachIndexed { index, row ->
            if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) newPage()
            if (index % 2 == 1) {
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, altRowPaint)
            }
            var x = MARGIN
            row.forEachIndexed { i, text ->
                val colWidth = columns.getOrNull(i)?.second ?: 60f
                val maxChars = (colWidth / 5.2f).toInt().coerceAtLeast(4)
                val truncated = if (text.length > maxChars) text.take(maxChars - 1) + "…" else text
                canvas.drawText(truncated, x + 4f, y + ROW_HEIGHT - 7f, cellPaint)
                x += colWidth
            }
            canvas.drawLine(MARGIN, y + ROW_HEIGHT, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, linePaint)
            y += ROW_HEIGHT
        }

        document.finishPage(page)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
