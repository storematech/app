package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.quizmaker.android.data.model.QuizDetailViewData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Renders the Quiz Detail View's per-respondent table as a simple paginated PDF. */
object QuizDetailPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 portrait at 72dpi — matches every other exported PDF in the app
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f
    private const val ROW_HEIGHT = 22f

    fun export(context: Context, data: QuizDetailViewData, branding: PdfBranding = PdfBranding.NONE): Intent {
        val columns = listOf(
            "Name" to 105f, "Email" to 145f, "Score" to 55f,
            "Correct" to 65f, "Time" to 55f, "Completed" to 106f
        )

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

        canvas.drawText("Quiz Detail View — ${data.quizTitle}", MARGIN, y + 16f, titlePaint)
        y += 30f
        val generatedAt = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
        canvas.drawText(
            "Responses: ${data.totalResponses}   Avg score: ${data.averageScore}%   Generated: $generatedAt",
            MARGIN, y, labelPaint
        )
        y += 18f

        drawHeaderRow()

        data.rows.forEachIndexed { index, row ->
            if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) newPage()
            if (index % 2 == 1) {
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, altRowPaint)
            }
            var x = MARGIN
            val timeLabel = row.timeSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-"
            val completedLabel = formatDateTime(row.completedAt)
            val cells = listOf(
                row.name, row.email, "${row.scorePercent}%",
                "${row.correctCount}/${data.gradedCount}", timeLabel, completedLabel
            )
            cells.forEachIndexed { i, text ->
                val maxChars = (columns[i].second / 5.2f).toInt().coerceAtLeast(4)
                val truncated = if (text.length > maxChars) text.take(maxChars - 1) + "…" else text
                canvas.drawText(truncated, x + 4f, y + ROW_HEIGHT - 7f, cellPaint)
                x += columns[i].second
            }
            canvas.drawLine(MARGIN, y + ROW_HEIGHT, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, linePaint)
            y += ROW_HEIGHT
        }

        document.finishPage(page)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = data.quizTitle.ifBlank { "quiz" }.replace(Regex("[^A-Za-z0-9]+"), "_")
        val file = File(exportsDir, "${safeName}_detail_view.pdf")
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
