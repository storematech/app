package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
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

    /**
     * Draws [text] treating [y] as the TOP of the line (not the baseline canvas.drawText expects),
     * then returns the y for the next line, spaced by this paint's own font metrics times
     * [multiplier] rather than a hand-picked pixel constant. Every previous magic-number increment
     * in this file (13f/14f/16f/18f/20f) was sized for one specific paint and drifted out of sync
     * whenever a different textSize/weight was drawn right after it — same call site, same paint,
     * same multiplier always gives correctly-proportioned spacing regardless of that paint's size.
     * Named distinctly from Canvas's own drawLine(x1,y1,x2,y2,paint) (used elsewhere in this file
     * for the actual divider/blank-answer geometry lines) so the two are never confused.
     */
    private fun Canvas.drawTextLine(text: String, x: Float, y: Float, paint: Paint, multiplier: Float = 1.3f): Float {
        val fm = paint.fontMetrics
        drawText(text, x, y - fm.ascent, paint)
        return y + (fm.descent - fm.ascent) * multiplier
    }

    private fun Paint.lineHeight(multiplier: Float = 1.3f): Float {
        val fm = fontMetrics
        return (fm.descent - fm.ascent) * multiplier
    }

    fun export(
        context: Context,
        quizTitle: String,
        questions: List<Question>,
        mode: MasterPaperMode,
        branding: PdfBranding = PdfBranding.NONE
    ): Intent {
        val document = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6D28D9"); textSize = 16f; isFakeBoldText = true }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 11f }
        val numberBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8B5CF6") }
        val numberTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 9f; isFakeBoldText = true }
        val typeBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4338CA"); textSize = 8f }
        val pointsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#92400E"); textSize = 8f }
        val questionTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111827"); textSize = 10.5f }
        val optionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#374151"); textSize = 10f }
        val correctOptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#16A34A"); textSize = 10f; isFakeBoldText = true }
        val correctBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#DCFCE7") }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 0.7f }
        val blankLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#C8C8C8"); strokeWidth = 0.7f }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 8f }

        // Reused for every wrapped multi-line block so line spacing within a paragraph is always
        // driven by that paint's own metrics — see drawTextLine()/lineHeight() above.
        val titleLineHeight = titlePaint.lineHeight(1.15f)
        val questionLineHeight = questionTextPaint.lineHeight(1.25f)
        val optionLineHeight = optionPaint.lineHeight(1.3f)

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

        val showAnswers = mode == MasterPaperMode.WITH_ANSWERS

        // Header
        wrapText(quizTitle, titlePaint, CONTENT_WIDTH).forEach { line ->
            y = canvas.drawTextLine(line, MARGIN, y, titlePaint, 1.15f)
        }
        y += 4f
        val subtitle = when (mode) {
            MasterPaperMode.OFFLINE -> "Offline Exam Paper"
            MasterPaperMode.WITH_ANSWERS -> "Master Paper with Answer Key"
            MasterPaperMode.WITHOUT_ANSWERS -> "Question Paper"
        }
        y = canvas.drawTextLine(subtitle, MARGIN, y, subtitlePaint, 1.3f)
        y = canvas.drawTextLine("Total: ${questions.size} Questions", MARGIN, y, subtitlePaint, 1.3f)
        y += 8f

        if (mode == MasterPaperMode.OFFLINE) {
            val fieldsTop = y
            canvas.drawTextLine("Name: ________________________________", MARGIN, y, optionPaint, 1.6f)
            canvas.drawTextLine("Roll No: ____________", MARGIN + 300f, y, optionPaint, 1.6f)
            y = fieldsTop + optionPaint.lineHeight(1.6f)
            val fieldsRow2Top = y
            canvas.drawTextLine("Date: ____________________", MARGIN, y, optionPaint, 1.6f)
            canvas.drawTextLine("Marks: ______ / ______", MARGIN + 300f, y, optionPaint, 1.6f)
            y = fieldsRow2Top + optionPaint.lineHeight(1.6f) + 8f
        }

        questions.forEachIndexed { index, q ->
            val qTextLines = wrapText(q.text, questionTextPaint, CONTENT_WIDTH - 8f)
            val optCount = if (q.type == QuestionType.SINGLE_CHOICE || q.type == QuestionType.MULTI_CHOICE) q.options.size else 1
            val estimatedHeight = 26f + qTextLines.size * questionLineHeight + optCount * optionLineHeight + 20f
            checkPage(estimatedHeight)

            val badgeTop = y
            canvas.drawRect(MARGIN, badgeTop, MARGIN + 22f, badgeTop + 14f, numberBgPaint)
            canvas.drawText("Q${index + 1}", MARGIN + 3f, badgeTop + 10.5f, numberTextPaint)
            val typeLabel = when (q.type) {
                QuestionType.MULTI_CHOICE -> "Multiple Select"
                QuestionType.FREE_TEXT -> "Free Text"
                QuestionType.FILL_IN_BLANK -> "Fill in the Blank"
                QuestionType.SINGLE_CHOICE -> "Single Choice"
            }
            canvas.drawText(typeLabel, MARGIN + 28f, badgeTop + 10f, typeBadgePaint)
            canvas.drawText("${q.points} pt${if (q.points > 1) "s" else ""}", MARGIN + 130f, badgeTop + 10f, pointsPaint)
            y += 22f

            qTextLines.forEach { line ->
                y = canvas.drawTextLine(line, MARGIN + 2f, y, questionTextPaint, 1.25f)
            }
            y += 4f

            when (q.type) {
                QuestionType.SINGLE_CHOICE, QuestionType.MULTI_CHOICE -> {
                    q.options.forEachIndexed { optIndex, opt ->
                        checkPage(optionLineHeight)
                        val isCorrect = opt.isCorrect
                        val rowTop = y
                        val rowHeight = optionLineHeight
                        if (isCorrect && showAnswers) {
                            canvas.drawRect(MARGIN + 2f, rowTop, MARGIN + CONTENT_WIDTH - 2f, rowTop + rowHeight, correctBgPaint)
                        }
                        val label = ('A' + optIndex)
                        val paint = if (isCorrect && showAnswers) correctOptionPaint else optionPaint
                        val baselineY = rowTop + rowHeight / 2f - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2f
                        canvas.drawText("$label. ${opt.text}", MARGIN + 6f, baselineY, paint)
                        if (isCorrect && showAnswers) {
                            canvas.drawText("✓ Correct", MARGIN + CONTENT_WIDTH - 55f, baselineY, correctOptionPaint)
                        }
                        y += rowHeight
                    }
                }
                else -> {
                    val answerHeight = optionLineHeight
                    checkPage(if (mode == MasterPaperMode.OFFLINE) answerHeight * 3 else answerHeight)
                    if (showAnswers) {
                        val rowTop = y
                        canvas.drawRect(MARGIN + 2f, rowTop, MARGIN + CONTENT_WIDTH - 2f, rowTop + answerHeight, correctBgPaint)
                        val baselineY = rowTop + answerHeight / 2f - (correctOptionPaint.fontMetrics.ascent + correctOptionPaint.fontMetrics.descent) / 2f
                        canvas.drawText("Answer: ${q.correctAnswer ?: "N/A"}", MARGIN + 6f, baselineY, correctOptionPaint)
                        y += answerHeight
                    } else {
                        val blankLines = if (mode == MasterPaperMode.OFFLINE) 3 else 2
                        repeat(blankLines) {
                            y += answerHeight * 0.7f
                            canvas.drawLine(MARGIN + 2f, y, MARGIN + CONTENT_WIDTH - 2f, y, blankLinePaint)
                            y += answerHeight * 0.3f
                        }
                    }
                }
            }

            y += 6f
            canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, linePaint)
            y += 16f
        }

        if (showAnswers) {
            checkPage(titleLineHeight + 10f)
            y = canvas.drawTextLine("Answer Key", MARGIN, y, titlePaint, 1.15f)
            y += 4f
            questions.forEachIndexed { index, q ->
                val summary = answerSummary(q)
                val lines = wrapText(summary, optionPaint, CONTENT_WIDTH - 40f)
                checkPage(optionLineHeight * lines.size)
                val rowTop = y
                lines.forEach { line ->
                    y = canvas.drawTextLine(line, MARGIN + 32f, y, correctOptionPaint, 1.3f)
                }
                val labelBaselineY = rowTop + optionPaint.lineHeight(1.3f) / 2f - (optionPaint.fontMetrics.ascent + optionPaint.fontMetrics.descent) / 2f
                canvas.drawText("Q${index + 1}:", MARGIN, labelBaselineY, optionPaint)
            }
        }

        checkPage(footerPaint.lineHeight(1.3f))
        canvas.drawTextLine(
            "Generated on ${SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())}",
            MARGIN, y, footerPaint, 1.3f
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
