package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MasterPaperMode { WITH_ANSWERS, WITHOUT_ANSWERS, OFFLINE }

/** Renders a quiz's questions as a paginated PDF — with/without an answer key, or a blank offline exam paper. */
object MasterPaperPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

    fun export(context: Context, quizTitle: String, questions: List<Question>, mode: MasterPaperMode): Intent {
        val document = PdfDocument()
        val titlePaint = Paint().apply { color = Color.parseColor("#6D28D9"); textSize = 16f; isFakeBoldText = true }
        val subtitlePaint = Paint().apply { color = Color.DKGRAY; textSize = 11f }
        val numberBgPaint = Paint().apply { color = Color.parseColor("#8B5CF6") }
        val numberTextPaint = Paint().apply { color = Color.WHITE; textSize = 9f; isFakeBoldText = true }
        val typeBadgePaint = Paint().apply { color = Color.parseColor("#4338CA"); textSize = 8f }
        val pointsPaint = Paint().apply { color = Color.parseColor("#92400E"); textSize = 8f }
        val questionTextPaint = Paint().apply { color = Color.parseColor("#111827"); textSize = 10.5f }
        val optionPaint = Paint().apply { color = Color.parseColor("#374151"); textSize = 10f }
        val correctOptionPaint = Paint().apply { color = Color.parseColor("#16A34A"); textSize = 10f; isFakeBoldText = true }
        val correctBgPaint = Paint().apply { color = Color.parseColor("#DCFCE7") }
        val linePaint = Paint().apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 0.7f }
        val blankLinePaint = Paint().apply { color = Color.parseColor("#C8C8C8"); strokeWidth = 0.7f }
        val footerPaint = Paint().apply { color = Color.GRAY; textSize = 8f }

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

        val showAnswers = mode == MasterPaperMode.WITH_ANSWERS

        // Header
        wrapText(quizTitle, titlePaint, CONTENT_WIDTH).forEach { line ->
            canvas.drawText(line, MARGIN, y + 12f, titlePaint)
            y += 18f
        }
        val subtitle = when (mode) {
            MasterPaperMode.OFFLINE -> "Offline Exam Paper"
            MasterPaperMode.WITH_ANSWERS -> "Master Paper with Answer Key"
            MasterPaperMode.WITHOUT_ANSWERS -> "Question Paper"
        }
        canvas.drawText(subtitle, MARGIN, y, subtitlePaint)
        y += 14f
        canvas.drawText("Total: ${questions.size} Questions", MARGIN, y, subtitlePaint)
        y += 20f

        if (mode == MasterPaperMode.OFFLINE) {
            canvas.drawText("Name: ________________________________", MARGIN, y, optionPaint)
            canvas.drawText("Roll No: ____________", MARGIN + 300f, y, optionPaint)
            y += 18f
            canvas.drawText("Date: ____________________", MARGIN, y, optionPaint)
            canvas.drawText("Marks: ______ / ______", MARGIN + 300f, y, optionPaint)
            y += 22f
        }

        questions.forEachIndexed { index, q ->
            val qTextLines = wrapText(q.text, questionTextPaint, CONTENT_WIDTH - 8f)
            val optCount = if (q.type == QuestionType.SINGLE_CHOICE || q.type == QuestionType.MULTI_CHOICE) q.options.size else 1
            val estimatedHeight = 22f + qTextLines.size * 13f + optCount * 16f + 18f
            checkPage(estimatedHeight)

            canvas.drawRect(MARGIN, y, MARGIN + 22f, y + 14f, numberBgPaint)
            canvas.drawText("Q${index + 1}", MARGIN + 3f, y + 11f, numberTextPaint)
            val typeLabel = when (q.type) {
                QuestionType.MULTI_CHOICE -> "Multiple Select"
                QuestionType.FREE_TEXT -> "Free Text"
                QuestionType.FILL_IN_BLANK -> "Fill in the Blank"
                QuestionType.SINGLE_CHOICE -> "Single Choice"
            }
            canvas.drawText(typeLabel, MARGIN + 28f, y + 10f, typeBadgePaint)
            canvas.drawText("${q.points} pt${if (q.points > 1) "s" else ""}", MARGIN + 130f, y + 10f, pointsPaint)
            y += 20f

            qTextLines.forEach { line ->
                canvas.drawText(line, MARGIN + 2f, y, questionTextPaint)
                y += 13f
            }
            y += 3f

            when (q.type) {
                QuestionType.SINGLE_CHOICE, QuestionType.MULTI_CHOICE -> {
                    q.options.forEachIndexed { optIndex, opt ->
                        checkPage(16f)
                        val isCorrect = opt.isCorrect
                        if (isCorrect && showAnswers) {
                            canvas.drawRect(MARGIN + 2f, y - 9f, MARGIN + CONTENT_WIDTH - 2f, y + 3f, correctBgPaint)
                        }
                        val label = ('A' + optIndex)
                        val paint = if (isCorrect && showAnswers) correctOptionPaint else optionPaint
                        canvas.drawText("$label. ${opt.text}", MARGIN + 6f, y, paint)
                        if (isCorrect && showAnswers) {
                            canvas.drawText("✓ Correct", MARGIN + CONTENT_WIDTH - 55f, y, correctOptionPaint)
                        }
                        y += 16f
                    }
                }
                else -> {
                    checkPage(if (mode == MasterPaperMode.OFFLINE) 44f else 16f)
                    if (showAnswers) {
                        canvas.drawRect(MARGIN + 2f, y - 9f, MARGIN + CONTENT_WIDTH - 2f, y + 3f, correctBgPaint)
                        canvas.drawText("Answer: ${q.correctAnswer ?: "N/A"}", MARGIN + 6f, y, correctOptionPaint)
                        y += 16f
                    } else {
                        val blankLines = if (mode == MasterPaperMode.OFFLINE) 3 else 2
                        repeat(blankLines) {
                            canvas.drawLine(MARGIN + 2f, y + 4f, MARGIN + CONTENT_WIDTH - 2f, y + 4f, blankLinePaint)
                            y += 14f
                        }
                    }
                }
            }

            y += 4f
            canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, linePaint)
            y += 14f
        }

        if (showAnswers) {
            checkPage(30f)
            canvas.drawText("Answer Key", MARGIN, y, titlePaint)
            y += 18f
            questions.forEachIndexed { index, q ->
                val summary = answerSummary(q)
                val lines = wrapText(summary, optionPaint, CONTENT_WIDTH - 40f)
                checkPage(13f * lines.size)
                canvas.drawText("Q${index + 1}:", MARGIN, y, optionPaint)
                lines.forEachIndexed { li, line ->
                    canvas.drawText(line, MARGIN + 32f, y, correctOptionPaint)
                    if (li < lines.lastIndex) y += 13f
                }
                y += 13f
            }
        }

        checkPage(20f)
        canvas.drawText(
            "Generated on ${SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())}",
            MARGIN, y, footerPaint
        )

        document.finishPage(page)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = quizTitle.ifBlank { "quiz" }.replace(Regex("[^A-Za-z0-9]+"), "_")
        val suffix = when (mode) {
            MasterPaperMode.WITH_ANSWERS -> "master-paper"
            MasterPaperMode.WITHOUT_ANSWERS -> "question-paper"
            MasterPaperMode.OFFLINE -> "offline-exam"
        }
        val file = File(exportsDir, "${safeName}_$suffix.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun answerSummary(q: Question): String = when (q.type) {
        QuestionType.SINGLE_CHOICE -> {
            val idx = q.options.indexOfFirst { it.isCorrect }
            if (idx >= 0) "${('A' + idx)}. ${q.options[idx].text}" else "N/A"
        }
        QuestionType.MULTI_CHOICE -> q.options.filter { it.isCorrect }.joinToString(", ") { it.text }.ifBlank { "N/A" }
        else -> q.correctAnswer?.ifBlank { null } ?: "N/A"
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
