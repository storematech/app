package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.quizmaker.android.data.model.QuizResponse
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Renders a filtered list of quiz responses (across all quizzes) as a paginated summary PDF. */
object ResponsesPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 portrait at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f
    private const val ROW_HEIGHT = 24f

    private val columns = listOf(
        "Quiz" to 140f, "User" to 155f, "Score" to 50f,
        "Status" to 65f, "Time" to 55f, "Completed" to 66f
    )

    fun export(
        context: Context,
        responses: List<QuizResponse>,
        quizTitleById: Map<String, String>,
        branding: PdfBranding = PdfBranding.NONE
    ): Intent {
        val document = PdfDocument()
        val template = branding.template
        val headerFilled = template == ReportTemplate.MODERN || template == ReportTemplate.BOLD
        val bodyFilled = template == ReportTemplate.BOLD
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (headerFilled) branding.accentColor else Color.BLACK; textSize = 16f; isFakeBoldText = true
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 10f }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (headerFilled) Color.WHITE else Color.BLACK; textSize = 9.5f; isFakeBoldText = true
        }
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = branding.accentColor }
        val headerRulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = branding.accentColor; strokeWidth = 1.5f }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (bodyFilled) Color.WHITE else Color.BLACK; textSize = 9.5f
        }
        val bodyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = branding.accentColor }
        val altRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = branding.accentColor; alpha = 18 }
        val boldAltRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; alpha = 25 }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 1f }
        val boldLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; alpha = 70; strokeWidth = 1f }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = PdfLetterhead.draw(canvas, MARGIN, PAGE_WIDTH - MARGIN, MARGIN, branding)

        fun drawHeaderRow() {
            var x = MARGIN
            when {
                headerFilled -> canvas.drawRect(x, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, headerBgPaint)
                template == ReportTemplate.CLASSIC -> canvas.drawLine(x, y + ROW_HEIGHT, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, headerRulePaint)
                else -> canvas.drawLine(x, y + ROW_HEIGHT, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, linePaint)
            }
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

        canvas.drawText("Responses", MARGIN, y + 16f, titlePaint)
        y += 30f
        val generatedAt = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Total: ${responses.size}   Generated: $generatedAt", MARGIN, y, labelPaint)
        y += 18f

        drawHeaderRow()

        responses.forEachIndexed { index, response ->
            if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) newPage()
            if (bodyFilled) {
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, bodyFillPaint)
                if (index % 2 == 1) canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, boldAltRowPaint)
            } else if (template == ReportTemplate.MODERN && index % 2 == 1) {
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, altRowPaint)
            }
            var x = MARGIN
            val status = if (response.cancelled) "Cancelled" else if (response.completed) "Completed" else "In Progress"
            val timeLabel = response.timeTakenSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-"
            val cells = listOf(
                quizTitleById[response.quizId] ?: "Unknown Quiz",
                response.userName?.ifBlank { null } ?: response.userEmail,
                "${response.score}%",
                status,
                timeLabel,
                formatDateTime(response.completedAt)
            )
            cells.forEachIndexed { i, text ->
                val maxChars = (columns[i].second / 5.2f).toInt().coerceAtLeast(4)
                val truncated = if (text.length > maxChars) text.take(maxChars - 1) + "…" else text
                canvas.drawText(truncated, x + 4f, y + ROW_HEIGHT - 7f, cellPaint)
                x += columns[i].second
            }
            canvas.drawLine(MARGIN, y + ROW_HEIGHT, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, if (bodyFilled) boldLinePaint else linePaint)
            y += ROW_HEIGHT
        }

        document.finishPage(page)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportsDir, "quiz_responses.pdf")
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
