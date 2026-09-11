package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionType
import ru.noties.jlatexmath.JLatexMathDrawable
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MasterPaperMode { WITH_ANSWERS, WITHOUT_ANSWERS, OFFLINE }

// One fragment of a word-wrap "word" -- either a plain-text run or a single $...$ formula,
// resolved/measured up front (see MasterPaperPdfExporter.resolveMixedWords). Kept at file scope,
// not nested inside the object below, because Kotlin doesn't allow a `typealias` as a class/object
// member -- only at file (top) level or local to a function.
private sealed class TextFragment(val width: Float) {
    class Plain(val text: String, width: Float) : TextFragment(width)
    class Formula(val drawable: Drawable, width: Float, val height: Float) : TextFragment(width)
}

/** One whitespace-delimited token, e.g. "($g=10$" -> [Plain("("), Formula(g=10)]. Kept together as
 *  a single wrap unit, same as any other "word" in plain word-wrap. */
private typealias MixedWord = List<TextFragment>

private fun MixedWord.totalWidth(): Float = sumOf { it.width.toDouble() }.toFloat()

/** Renders a quiz's questions as a paginated PDF — with/without an answer key, or a blank offline exam paper. */
object MasterPaperPdfExporter {

    // Not private: OfflineExamPaperPreviewScreen's live preview box uses this exact aspect ratio
    // so the on-screen preview is never a different shape than the actual exported page.
    const val PAGE_WIDTH = 595 // A4 at 72dpi
    const val PAGE_HEIGHT = 842
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

    // ---- Mixed text+math layout (single-dollar $...$ formulas embedded in question/option text) --
    //
    // A PdfDocument's Canvas has no rich-text layout of its own -- canvas.drawText() only ever
    // draws plain glyphs, so a question like "coefficient of friction 0.4 ($g=10$ m/s^2)" used to
    // come out with the literal "$g=10$" characters in the exported/printed PDF, unlike the in-app
    // screens (see MathText.kt) which typeset it as a real formula. Fixed here the same way: each
    // $...$ span is rendered via JLatexMathDrawable -- the same JLaTeXMath wrapper Markwon's
    // ext-latex uses under MathText -- which is a real android.graphics.drawable.Drawable, so it
    // can draw() onto any Canvas, a PDF page's canvas included, with no async/view-hierarchy needed.
    //
    // Word-wrap keeps a $...$ span glued to whatever plain-text characters share its whitespace-
    // delimited "word" (so "($g=10$" wraps as one atomic unit, exactly like plain text word-wrap
    // already treats any other word) and measures/wraps by each word's total width across its
    // mixed fragments. (TextFragment/MixedWord/totalWidth() live at file scope above -- see there.)

    private val MATH_SPAN = Regex("\\$([^$\\n]+?)\\$")

    private fun buildFormulaDrawable(latex: String, textSizePx: Float, colorInt: Int): Drawable? = try {
        JLatexMathDrawable.builder(latex).textSize(textSizePx).color(colorInt).build()
    } catch (t: Throwable) {
        // A single malformed formula (LaTeX syntax JLaTeXMath can't parse) must never break the
        // whole export -- fall back to that one formula's raw "$latex$" source as plain text
        // rather than failing the whole PDF over one bad question, same philosophy as MathText's
        // own per-render fallback.
        null
    }

    /** Tokenizes [text] into whitespace-delimited words, each split further into plain/formula
     *  fragments, every fragment already measured/built against [paint] so wrapping and drawing
     *  both work off this same resolved shape. */
    private fun resolveMixedWords(text: String, paint: Paint): List<MixedWord> {
        if (text.isBlank()) return listOf(listOf(TextFragment.Plain("", 0f)))
        val textSizePx = paint.textSize
        val colorInt = paint.color
        return text.split(Regex("\\s+")).filter { it.isNotEmpty() }.map { raw ->
            val fragments = mutableListOf<TextFragment>()
            var last = 0
            for (m in MATH_SPAN.findAll(raw)) {
                if (m.range.first > last) {
                    val plain = superscriptBareExponents(raw.substring(last, m.range.first))
                    fragments.add(TextFragment.Plain(plain, paint.measureText(plain)))
                }
                val latex = m.groupValues[1]
                val drawable = buildFormulaDrawable(latex, textSizePx, colorInt)
                fragments.add(
                    if (drawable != null) {
                        TextFragment.Formula(drawable, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
                    } else {
                        val rawFormula = "$$latex$"
                        TextFragment.Plain(rawFormula, paint.measureText(rawFormula))
                    }
                )
                last = m.range.last + 1
            }
            if (last < raw.length) {
                val plain = superscriptBareExponents(raw.substring(last))
                fragments.add(TextFragment.Plain(plain, paint.measureText(plain)))
            }
            if (fragments.isEmpty()) {
                val plain = superscriptBareExponents(raw)
                fragments.add(TextFragment.Plain(plain, paint.measureText(plain)))
            }
            fragments
        }
    }

    /** Greedy word-wrap over already-measured [words], mirroring wrapText()'s algorithm but
     *  summing fragment widths per word instead of measuring a single plain string. */
    private fun wrapMixedWords(words: List<MixedWord>, spaceWidth: Float, maxWidth: Float): List<List<MixedWord>> {
        val lines = mutableListOf<List<MixedWord>>()
        var current = mutableListOf<MixedWord>()
        var currentWidth = 0f
        for (word in words) {
            val wordWidth = word.totalWidth()
            val candidateWidth = if (current.isEmpty()) wordWidth else currentWidth + spaceWidth + wordWidth
            if (candidateWidth > maxWidth && current.isNotEmpty()) {
                lines.add(current)
                current = mutableListOf(word)
                currentWidth = wordWidth
            } else {
                current.add(word)
                currentWidth = candidateWidth
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines
    }

    /** Draws already-wrapped/resolved [words] left-to-right starting at ([x], [baselineY]) with no
     *  further wrapping -- the single-line primitive both drawMixedLine() below and single-line
     *  callers (option rows, which never wrap) share. Formula fragments are bottom-aligned to
     *  [baselineY], same as how an inline image defaults to baseline alignment next to text. */
    private fun Canvas.drawMixedRun(words: List<MixedWord>, x: Float, baselineY: Float, paint: Paint, spaceWidth: Float) {
        var cursorX = x
        words.forEachIndexed { wordIndex, word ->
            word.forEach { fragment ->
                when (fragment) {
                    is TextFragment.Plain -> drawText(fragment.text, cursorX, baselineY, paint)
                    is TextFragment.Formula -> {
                        val left = cursorX.toInt()
                        val top = (baselineY - fragment.height).toInt()
                        fragment.drawable.setBounds(left, top, left + fragment.width.toInt(), top + fragment.height.toInt())
                        fragment.drawable.draw(this)
                    }
                }
                cursorX += fragment.width
            }
            if (wordIndex != words.lastIndex) cursorX += spaceWidth
        }
    }

    /** Draws one already-wrapped line of mixed words at [y] (top of the line, same "y is the top"
     *  contract as Canvas.drawTextLine() above), returning the y for the next line -- tall enough
     *  to clear the tallest formula on the line, not just [paint]'s own font metrics. */
    private fun Canvas.drawMixedLine(line: List<MixedWord>, x: Float, y: Float, paint: Paint, spaceWidth: Float, multiplier: Float = 1.3f): Float {
        val fm = paint.fontMetrics
        val textLineHeight = (fm.descent - fm.ascent) * multiplier
        val tallestFormula = line.maxOfOrNull { word ->
            word.filterIsInstance<TextFragment.Formula>().maxOfOrNull { it.height } ?: 0f
        } ?: 0f
        val lineHeight = maxOf(textLineHeight, tallestFormula * 1.15f)
        // Extra height (if any) is split above/below so a line with a tall formula doesn't shove
        // its plain text down to the very bottom of the line box.
        val baselineY = y - fm.ascent + (lineHeight - textLineHeight) / 2f
        drawMixedRun(line, x, baselineY, paint, spaceWidth)
        return y + lineHeight
    }

    /** Builds the PDF and writes it to a cache file, shared by [export] (wraps it in a share Intent)
     *  and [renderPreviewBitmap] (rasterizes page 1 for an on-screen live preview) — both need the
     *  exact same drawing code so the preview is never able to drift out of sync with the real
     *  download. */
    private fun buildPdfFile(
        context: Context,
        quizTitle: String,
        questions: List<Question>,
        mode: MasterPaperMode,
        branding: PdfBranding
    ): File {
        val document = PdfDocument()
        val template = branding.template
        val badgeFilled = template == ReportTemplate.MODERN || template == ReportTemplate.BOLD
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (badgeFilled) branding.accentColor else Color.BLACK; textSize = 16f; isFakeBoldText = true
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 11f }
        val numberBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = branding.accentColor }
        val numberRulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = branding.accentColor; strokeWidth = 1.5f }
        // MINIMAL never uses the accent color for a badge — a plain neutral outline keeps the
        // question-number box's structure without any brand color leaking in.
        val numberOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D1D5DB"); style = Paint.Style.STROKE; strokeWidth = 0.75f
        }
        val numberTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (badgeFilled) Color.WHITE else Color.BLACK; textSize = 9f; isFakeBoldText = true
        }
        val typeBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = branding.accentColor; textSize = 8f }
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

        val spaceWidth = questionTextPaint.measureText(" ")
        val optionSpaceWidth = optionPaint.measureText(" ")

        questions.forEachIndexed { index, q ->
            val qWords = resolveMixedWords(q.text, questionTextPaint)
            val qLines = wrapMixedWords(qWords, spaceWidth, CONTENT_WIDTH - 8f)
            val optCount = if (q.type == QuestionType.SINGLE_CHOICE || q.type == QuestionType.MULTI_CHOICE) q.options.size else 1
            val estimatedHeight = 26f + qLines.size * questionLineHeight + optCount * optionLineHeight + 20f
            checkPage(estimatedHeight)

            val badgeTop = y
            when {
                badgeFilled -> canvas.drawRect(MARGIN, badgeTop, MARGIN + 22f, badgeTop + 14f, numberBgPaint)
                template == ReportTemplate.CLASSIC -> canvas.drawLine(MARGIN, badgeTop + 14f, MARGIN + 22f, badgeTop + 14f, numberRulePaint)
                else -> canvas.drawRect(MARGIN, badgeTop, MARGIN + 22f, badgeTop + 14f, numberOutlinePaint) // MINIMAL
            }
            canvas.drawText("Q${index + 1}", MARGIN + 3f, badgeTop + 10.5f, numberTextPaint)
            val typeLabel = when (q.type) {
                QuestionType.MULTI_CHOICE -> "Multiple Select"
                QuestionType.FREE_TEXT -> "Free Text"
                QuestionType.FILL_IN_BLANK -> "Fill in the Blank"
                QuestionType.SINGLE_CHOICE -> "Single Choice"
            }
            canvas.drawText(typeLabel, MARGIN + 28f, badgeTop + 10f, typeBadgePaint)
            canvas.drawText("${q.points.formatPoints()} pt${if (q.points > 1) "s" else ""}", MARGIN + 130f, badgeTop + 10f, pointsPaint)
            y += 22f

            qLines.forEach { line ->
                y = canvas.drawMixedLine(line, MARGIN + 2f, y, questionTextPaint, spaceWidth, 1.25f)
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
                        val prefix = "$label. "
                        canvas.drawText(prefix, MARGIN + 6f, baselineY, paint)
                        canvas.drawMixedRun(
                            resolveMixedWords(opt.text, paint),
                            MARGIN + 6f + paint.measureText(prefix),
                            baselineY,
                            paint,
                            optionSpaceWidth
                        )
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
                val words = resolveMixedWords(summary, correctOptionPaint)
                val lines = wrapMixedWords(words, optionSpaceWidth, CONTENT_WIDTH - 40f)
                checkPage(optionLineHeight * lines.size)
                val rowTop = y
                lines.forEach { line ->
                    y = canvas.drawMixedLine(line, MARGIN + 32f, y, correctOptionPaint, optionSpaceWidth, 1.3f)
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
        return file
    }

    fun export(
        context: Context,
        quizTitle: String,
        questions: List<Question>,
        mode: MasterPaperMode,
        branding: PdfBranding = PdfBranding.NONE
    ): Intent {
        val file = buildPdfFile(context, quizTitle, questions, mode, branding)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Renders page 1 of the exact same PDF [export] would produce, rasterized to a [Bitmap] for an
     * on-screen live preview — since it's built from the identical [buildPdfFile] drawing code, the
     * preview can never drift out of sync with what Download actually produces. Runs real file I/O
     * and PDF rendering, so callers must invoke this off the main thread. Returns null (never
     * throws) if generation/rendering fails, so a preview glitch never blocks the real download.
     */
    fun renderPreviewBitmap(
        context: Context,
        quizTitle: String,
        questions: List<Question>,
        mode: MasterPaperMode,
        branding: PdfBranding,
        widthPx: Int
    ): Bitmap? = runCatching {
        val file = buildPdfFile(context, quizTitle, questions, mode, branding)
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) return@runCatching null
                renderer.openPage(0).use { page ->
                    val scale = widthPx.toFloat() / page.width
                    val heightPx = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    val matrix = Matrix().apply { setScale(scale, scale) }
                    page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap
                }
            }
        }
    }.getOrNull()

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
