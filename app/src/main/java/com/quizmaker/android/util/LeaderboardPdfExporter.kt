package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.quizmaker.android.data.model.LeaderboardData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Renders a Leaderboard as a simple paginated table PDF and returns a share Intent for it. */
object LeaderboardPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f
    private const val ROW_HEIGHT = 24f

    private val columns = listOf("Rank" to 45f, "Name" to 125f, "Email" to 175f, "Points" to 80f, "Score" to 55f, "Time" to 51f)

    fun export(context: Context, quizTitle: String, data: LeaderboardData): Intent {
        val document = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 18f; isFakeBoldText = true }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 11f }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 10f; isFakeBoldText = true }
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6366F1") }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 10f }
        val altRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F5F5FB") }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 1f }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN

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

        canvas.drawText("Leaderboard — $quizTitle", MARGIN, y + 18f, titlePaint)
        y += 34f
        val generatedAt = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Participants: ${data.totalAttempts}   Avg score: ${data.averageScore}%   Generated: $generatedAt", MARGIN, y, labelPaint)
        y += 20f

        drawHeaderRow()

        data.entries.forEachIndexed { index, entry ->
            if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) {
                newPage()
            }
            if (index % 2 == 1) {
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, altRowPaint)
            }
            var x = MARGIN
            val percent = if (data.maxPoints > 0) (entry.earnedPoints * 100 / data.maxPoints) else 0
            val timeLabel = entry.timeTakenSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-"
            val cells = listOf(
                "#${entry.rank}", entry.userName, entry.userEmail,
                "${entry.earnedPoints}/${data.maxPoints}", "$percent%", timeLabel
            )
            cells.forEachIndexed { i, text ->
                val maxChars = (columns[i].second / 5.5f).toInt().coerceAtLeast(4)
                val truncated = if (text.length > maxChars) text.take(maxChars - 1) + "…" else text
                canvas.drawText(truncated, x + 4f, y + ROW_HEIGHT - 7f, cellPaint)
                x += columns[i].second
            }
            canvas.drawLine(MARGIN, y + ROW_HEIGHT, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, linePaint)
            y += ROW_HEIGHT
        }

        document.finishPage(page)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = quizTitle.ifBlank { "quiz" }.replace(Regex("[^A-Za-z0-9]+"), "_")
        val file = File(exportsDir, "leaderboard_$safeName.pdf")
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
