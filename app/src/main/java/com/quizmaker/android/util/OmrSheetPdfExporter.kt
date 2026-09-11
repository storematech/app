package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.quizmaker.android.data.model.OmrBubble
import com.quizmaker.android.data.model.OmrFiducial
import com.quizmaker.android.data.model.OmrLayout
import com.quizmaker.android.data.model.OmrPage
import com.quizmaker.android.data.model.OmrQuestionLayout
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionType
import java.io.File
import java.io.FileOutputStream

/** Bundles the share [Intent] (same convention as every other *PdfExporter — see
 *  [QrFlyerPdfExporter]) together with the [OmrLayout] that was built while drawing the PDF, so the
 *  caller can persist it via `OmrRepository.saveLayout` right after sharing/printing the sheet. */
data class OmrSheetExportResult(val shareIntent: Intent, val layout: OmrLayout)

/**
 * Renders a printable OMR ("bubble sheet") answer sheet for a quiz's objective (choice) questions,
 * plus ruled blank-answer space for any free-text/fill-in-the-blank questions mixed into the same
 * quiz (those are never auto-graded — see ManualMarkingRepository — but still need a place on the
 * paper to answer them by hand). Returns both a share [Intent] for the rendered PDF and the
 * [OmrLayout] describing where every fiducial marker and answer bubble ended up, in PDF-point
 * coordinates — the (future) scanning/grading pass reads that layout back to know what a mark at a
 * given position on the photographed paper actually means.
 *
 * Mirrors [MasterPaperPdfExporter]'s page constants/pagination shape and [QrFlyerPdfExporter]'s
 * "render then wrap in a share Intent" convention. Word-wrap here is a plain-text
 * measureText()-based wrap (see [wrapText] below) rather than [MasterPaperPdfExporter]'s
 * mixed text+LaTeX word-wrap helpers (TextFragment/MixedWord/resolveMixedWords) — those are
 * file-private to that file and not shared across files, and a $...$ formula rendered as raw
 * literal text on this one exporter is an acceptable simplification: a bubble sheet's own answer
 * bubbles are what actually gets graded, so a formula in a question's prose not being LaTeX-
 * typeset on this specific export doesn't block grading the way it would on a graded answer key.
 */
object OmrSheetPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN

    // Deliberately smaller than MARGIN and anchored to the raw page edge (not the content margin)
    // so the 4 corner fiducials never collide with the letterhead (drawn from MARGIN,MARGIN) or any
    // question content (drawn within MARGIN..PAGE_WIDTH-MARGIN / MARGIN..PAGE_HEIGHT-MARGIN).
    private const val FIDUCIAL_INSET = 14f
    private const val FIDUCIAL_SIZE = 14f

    private const val BUBBLE_RADIUS = 7f
    private const val BUBBLE_SPACING = 42f

    // ---- Pagination-safe text-drawing primitives — copied verbatim from MasterPaperPdfExporter
    // (Kotlin file-scoped/private declarations aren't shared across files, so this file keeps its
    // own copy rather than importing) ----------------------------------------------------------

    /**
     * Draws [text] treating [y] as the TOP of the line (not the baseline canvas.drawText expects),
     * then returns the y for the next line, spaced by this paint's own font metrics times
     * [multiplier] rather than a hand-picked pixel constant.
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

    /** Plain whitespace word-wrap — see this object's KDoc for why this file uses a simplified
     *  plain-text wrap instead of MasterPaperPdfExporter's mixed text+LaTeX wrap helpers. */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
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

    fun export(
        context: Context,
        quizTitle: String,
        questions: List<Question>,
        pdfBranding: PdfBranding
    ): OmrSheetExportResult {
        val document = PdfDocument()

        val fiducialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.FILL }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 16f; isFakeBoldText = true }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 11f }
        val accentRulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pdfBranding.accentColor; strokeWidth = 1.5f }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 10.5f }
        val fieldLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9CA3AF"); strokeWidth = 0.8f }
        val questionTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111827"); textSize = 10.5f }
        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; style = Paint.Style.STROKE; strokeWidth = 1.2f }
        val bubbleLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 8.5f; textAlign = Paint.Align.CENTER
        }
        val blankLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#C8C8C8"); strokeWidth = 0.7f }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 0.7f }

        val questionLineHeight = questionTextPaint.lineHeight(1.25f)
        val bubbleRowHeight = BUBBLE_RADIUS * 2f + 18f
        val blankAnswerLineGap = 16f
        val blankAnswerLinesCount = 3

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas

        val finishedPages = mutableListOf<OmrPage>()
        var currentFiducials: List<OmrFiducial> = emptyList()
        var currentQuestions = mutableListOf<OmrQuestionLayout>()
        var y: Float

        fun drawFiducials(c: Canvas): List<OmrFiducial> {
            val corners = listOf(
                FIDUCIAL_INSET to FIDUCIAL_INSET, // top-left
                (PAGE_WIDTH - FIDUCIAL_INSET - FIDUCIAL_SIZE) to FIDUCIAL_INSET, // top-right
                FIDUCIAL_INSET to (PAGE_HEIGHT - FIDUCIAL_INSET - FIDUCIAL_SIZE), // bottom-left
                (PAGE_WIDTH - FIDUCIAL_INSET - FIDUCIAL_SIZE) to (PAGE_HEIGHT - FIDUCIAL_INSET - FIDUCIAL_SIZE) // bottom-right
            )
            return corners.map { (left, top) ->
                c.drawRect(left, top, left + FIDUCIAL_SIZE, top + FIDUCIAL_SIZE, fiducialPaint)
                OmrFiducial(cx = left + FIDUCIAL_SIZE / 2f, cy = top + FIDUCIAL_SIZE / 2f, size = FIDUCIAL_SIZE)
            }
        }

        currentFiducials = drawFiducials(canvas)
        y = MARGIN
        y = PdfLetterhead.draw(canvas, MARGIN, PAGE_WIDTH - MARGIN, y, pdfBranding)

        fun newPage() {
            finishedPages.add(OmrPage(fiducials = currentFiducials, questions = currentQuestions.toList()))
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            currentFiducials = drawFiducials(canvas)
            currentQuestions = mutableListOf()
            y = MARGIN
        }

        fun checkPage(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN) newPage()
        }

        // ---- Header (page 1 only) ----
        y = canvas.drawTextLine(quizTitle, MARGIN, y, titlePaint, 1.15f)
        y += 2f
        y = canvas.drawTextLine("Answer Sheet", MARGIN, y, subtitlePaint, 1.3f)
        y += 6f
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, accentRulePaint)
        y += 18f

        listOf("Student Name:", "Date:").forEach { label ->
            val fm = labelPaint.fontMetrics
            val baseline = y - fm.ascent
            canvas.drawText(label, MARGIN, baseline, labelPaint)
            val lineStartX = MARGIN + labelPaint.measureText(label) + 10f
            canvas.drawLine(lineStartX, baseline, MARGIN + 260f, baseline, fieldLinePaint)
            y += labelPaint.lineHeight(1.8f)
        }
        y += 10f

        // ---- Questions, in the exact order given (persisted order must match how questions are
        // always fetched — see QuizRepository.getQuestionsForQuiz) ----
        questions.forEachIndexed { index, q ->
            val isChoice = q.type == QuestionType.SINGLE_CHOICE || q.type == QuestionType.MULTI_CHOICE
            val qLines = wrapText("Q${index + 1}. ${q.text}", questionTextPaint, CONTENT_WIDTH)
            val answerAreaHeight = if (isChoice) bubbleRowHeight else blankAnswerLinesCount * blankAnswerLineGap
            val estimatedHeight = qLines.size * questionLineHeight + answerAreaHeight + 20f
            checkPage(estimatedHeight)

            qLines.forEach { line -> y = canvas.drawTextLine(line, MARGIN, y, questionTextPaint, 1.25f) }
            y += 6f

            if (isChoice) {
                val bubbles = mutableListOf<OmrBubble>()
                val rowCy = y + BUBBLE_RADIUS
                q.options.forEachIndexed { optIndex, opt ->
                    val cx = MARGIN + 10f + BUBBLE_RADIUS + optIndex * BUBBLE_SPACING
                    canvas.drawCircle(cx, rowCy, BUBBLE_RADIUS, bubblePaint)
                    val label = ('A' + optIndex).toString()
                    val fm = bubbleLabelPaint.fontMetrics
                    canvas.drawText(label, cx, rowCy - (fm.ascent + fm.descent) / 2f, bubbleLabelPaint)
                    bubbles.add(OmrBubble(optionId = opt.id, label = label, cx = cx, cy = rowCy, radius = BUBBLE_RADIUS))
                }
                y = rowCy + BUBBLE_RADIUS + 14f
                currentQuestions.add(OmrQuestionLayout(questionId = q.id, type = q.type.value, bubbles = bubbles))
            } else {
                repeat(blankAnswerLinesCount) {
                    y += blankAnswerLineGap
                    canvas.drawLine(MARGIN + 2f, y, MARGIN + CONTENT_WIDTH - 2f, y, blankLinePaint)
                }
                y += 10f
                // Pushed with an empty bubble list anyway — needed so the layout's question list
                // stays 1:1 (position and count) with [questions], letting a later pass reconstruct
                // answers by index even for non-bubble questions.
                currentQuestions.add(OmrQuestionLayout(questionId = q.id, type = q.type.value, bubbles = emptyList()))
            }

            y += 4f
            canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, dividerPaint)
            y += 14f
        }

        finishedPages.add(OmrPage(fiducials = currentFiducials, questions = currentQuestions.toList()))
        document.finishPage(page)

        val layout = OmrLayout(pageWidth = PAGE_WIDTH.toFloat(), pageHeight = PAGE_HEIGHT.toFloat(), pages = finishedPages)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = quizTitle.ifBlank { "quiz" }.replace(Regex("[^A-Za-z0-9]+"), "_")
        val file = File(exportsDir, "${safeName}_omr_sheet.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        return OmrSheetExportResult(shareIntent = intent, layout = layout)
    }
}
