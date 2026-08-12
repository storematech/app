package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.quizmaker.android.data.model.QuizAnalysisData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Renders the Quiz Analysis per-question breakdown as a paginated PDF. */
object QuizAnalysisPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 portrait at 72dpi — matches every other exported PDF in the app
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
    private const val BAR_HEIGHT = 7f

    fun export(context: Context, data: QuizAnalysisData, branding: PdfBranding = PdfBranding.NONE): Intent {
        val document = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6D28D9"); textSize = 16f; isFakeBoldText = true }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 10f }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111827"); textSize = 13f; isFakeBoldText = true }
        val questionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111827"); textSize = 10.5f; isFakeBoldText = true }
        val statsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B7280"); textSize = 9f }
        val barBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E7EB") }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9CA3AF"); textSize = 8f }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = PdfLetterhead.draw(canvas, MARGIN, PAGE_WIDTH - MARGIN, MARGIN, branding)

        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
        }

        fun checkPage(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN) newPage()
        }

        // Header
        canvas.drawText("Question Performance", MARGIN, y + 14f, titlePaint)
        y += 20f
        if (data.quizTitle.isNotBlank()) {
            canvas.drawText(data.quizTitle, MARGIN, y + 8f, subtitlePaint)
            y += 16f
        }
        val generatedAt = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
        val avgTimeLabel = data.averageTimeSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-"
        canvas.drawText(
            "Responses: ${data.totalResponses}   Avg score: ${data.averageScore}%   Avg time: $avgTimeLabel   Generated: $generatedAt",
            MARGIN, y, subtitlePaint
        )
        y += 20f

        checkPage(20f)
        canvas.drawText("Question Breakdown", MARGIN, y, sectionPaint)
        y += 18f

        data.questions.forEach { stat ->
            val qLines = wrapText("Q${stat.order}. ${stat.text}", questionPaint, CONTENT_WIDTH - 50f)
            val blockHeight = qLines.size * 13f + BAR_HEIGHT + 26f
            checkPage(blockHeight + 6f)

            var qy = y + 10f
            val percentColor = when {
                stat.correctPercent >= 70 -> "#22C55E"
                stat.correctPercent >= 40 -> "#F59E0B"
                else -> "#EF4444"
            }
            val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(percentColor); textSize = 12f; isFakeBoldText = true }

            qLines.forEachIndexed { li, line ->
                canvas.drawText(line, MARGIN, qy, questionPaint)
                if (li == 0 && !stat.isUngraded) {
                    canvas.drawText("${stat.correctPercent}%", MARGIN + CONTENT_WIDTH - 40f, qy, percentPaint)
                }
                qy += 13f
            }
            qy += 2f

            if (stat.isUngraded) {
                canvas.drawText(
                    "Ungraded — ${stat.attempted} attempted, ${stat.skippedCount} skipped",
                    MARGIN, qy, statsPaint
                )
                qy += BAR_HEIGHT + 8f
            } else {
                canvas.drawRect(MARGIN, qy, MARGIN + CONTENT_WIDTH, qy + BAR_HEIGHT, barBgPaint)
                canvas.drawRect(
                    MARGIN, qy, MARGIN + CONTENT_WIDTH * (stat.correctPercent / 100f), qy + BAR_HEIGHT,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(percentColor) }
                )
                qy += BAR_HEIGHT + 12f
                canvas.drawText(
                    "${stat.correctCount} correct · ${stat.wrongCount} wrong · ${stat.skippedCount} skipped",
                    MARGIN, qy, statsPaint
                )
                qy += 8f
            }

            y = qy + 8f
        }

        checkPage(16f)
        canvas.drawText("Generated on $generatedAt", MARGIN, y, footerPaint)

        document.finishPage(page)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = data.quizTitle.ifBlank { "quiz" }.replace(Regex("[^A-Za-z0-9]+"), "_")
        val file = File(exportsDir, "${safeName}_analysis.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                lines.add(current.toString())
                current = StringBuilder(word)
            } else {
                current = StringBuilder(candidate)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}
