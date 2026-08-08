package com.quizmaker.android.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.quizmaker.android.data.model.AnswerStatus
import com.quizmaker.android.data.model.ResponseDetailData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Renders one participant's full question-by-question breakdown as a paginated PDF. */
object ResponseDetailPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

    fun export(context: Context, data: ResponseDetailData): File {
        val document = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6D28D9"); textSize = 16f; isFakeBoldText = true }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B7280"); textSize = 8f }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111827"); textSize = 9.5f; isFakeBoldText = true }
        val summaryBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F5F3FF") }
        val questionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111827"); textSize = 10f; isFakeBoldText = true }
        val labelBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4B5563"); textSize = 9f; isFakeBoldText = true }
        val answerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111827"); textSize = 9f }
        val correctPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#16A34A"); textSize = 9f }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9CA3AF"); textSize = 8f }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN

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
        canvas.drawText("Response Details", MARGIN, y + 12f, titlePaint)
        y += 16f
        if (data.quizTitle.isNotBlank()) {
            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 10f }
            canvas.drawText(data.quizTitle, MARGIN, y + 10f, subtitlePaint)
            y += 16f
        }
        y += 8f

        // Summary box
        canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 58f, summaryBgPaint)
        val timeLabel = data.timeSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-"
        val summaryItems = listOf(
            "Student" to data.userName,
            "Email" to data.userEmail,
            "Score" to "${data.scorePercent}%",
            "Time Taken" to timeLabel
        )
        val colW = CONTENT_WIDTH / 2
        summaryItems.forEachIndexed { index, (label, value) ->
            val col = index % 2
            val row = index / 2
            val bx = MARGIN + 8f + col * colW
            val by = y + 14f + row * 26f
            canvas.drawText(label, bx, by, labelPaint)
            canvas.drawText(value, bx, by + 12f, valuePaint)
        }
        y += 68f

        // Section header
        checkPage(20f)
        canvas.drawText("Question by Question Analysis", MARGIN, y, titlePaint)
        y += 16f

        val answerLabel = "Submitted Answer:"
        val correctLabel = "Correct:"
        val labelGap = 6f
        val answerValueIndent = 10f + labelBoldPaint.measureText(answerLabel) + labelGap
        val correctValueIndent = 10f + labelBoldPaint.measureText(correctLabel) + labelGap

        data.answers.forEachIndexed { index, answer ->
            val (bandColor, statusLabel) = when (answer.status) {
                AnswerStatus.CORRECT -> "#16A34A" to "Correct"
                AnswerStatus.PARTIAL -> "#D97706" to "Partial"
                AnswerStatus.INCORRECT -> "#DC2626" to "Incorrect"
                AnswerStatus.UNGRADED -> "#6B7280" to "Ungraded"
            }
            val bandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor(bandColor) }

            val qLines = wrapText("Q${index + 1}. ${answer.questionText}", questionPaint, CONTENT_WIDTH - 12f)
            val ansLines = wrapText(answer.studentAnswer, answerPaint, CONTENT_WIDTH - answerValueIndent)
            val corLines = wrapText(answer.correctAnswer, correctPaint, CONTENT_WIDTH - correctValueIndent)
            val blockHeight = qLines.size * 12f + ansLines.size * 13f + corLines.size * 13f + 26f

            checkPage(blockHeight + 6f)

            canvas.drawRect(MARGIN, y, MARGIN + 3f, y + blockHeight, bandPaint)
            var qy = y + 10f
            qLines.forEach { line -> canvas.drawText(line, MARGIN + 10f, qy, questionPaint); qy += 12f }
            qy += 4f

            canvas.drawText(answerLabel, MARGIN + 10f, qy, labelBoldPaint)
            ansLines.forEachIndexed { li, line ->
                canvas.drawText(line, if (li == 0) MARGIN + answerValueIndent else MARGIN + 14f, qy, answerPaint)
                qy += 13f
            }
            qy += 2f

            canvas.drawText(correctLabel, MARGIN + 10f, qy, labelBoldPaint)
            corLines.forEachIndexed { li, line ->
                canvas.drawText(line, if (li == 0) MARGIN + correctValueIndent else MARGIN + 14f, qy, correctPaint)
                qy += 13f
            }
            qy += 2f

            val pointsLabel = if (answer.status == AnswerStatus.UNGRADED) "Ungraded" else "${answer.pointsEarned}/${answer.maxPoints} pts"
            canvas.drawText("$pointsLabel  •  $statusLabel", MARGIN + 10f, qy, labelPaint)

            y += blockHeight + 8f
        }

        checkPage(16f)
        canvas.drawText(
            "Generated on ${SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())}",
            MARGIN, y, footerPaint
        )

        document.finishPage(page)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = data.userEmail.ifBlank { "response" }.replace(Regex("[^A-Za-z0-9]+"), "_")
        val file = File(exportsDir, "response_$safeName.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
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
