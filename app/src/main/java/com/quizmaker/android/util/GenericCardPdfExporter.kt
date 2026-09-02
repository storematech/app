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

/** One response's worth of "Label: Value" fields, rendered as a single card by [GenericCardPdfExporter]. */
data class PdfCardData(
    val headline: String,
    val subheadline: String? = null,
    val fields: List<Pair<String, String>>,
    val footer: String? = null
)

/**
 * Renders one card per record instead of one table row per record — the counterpart to
 * GenericTablePdfExporter for exports where the number of columns is genuinely unbounded
 * (Onboarding/Feedback submissions carry whatever custom fields that particular form's creator
 * defined). A fixed-width table can't reliably fit an arbitrary field count without truncating
 * headers or squeezing columns until they collide — exactly what happened before this existed.
 * Each card instead stacks its label/value pairs vertically, so a form with more fields just makes
 * its cards taller, never narrower than legible. Same letterhead/pagination/branding plumbing as
 * every other *PdfExporter.
 */
object GenericCardPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 portrait at 72dpi — matches every other exported PDF in the app
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 32f

    fun export(
        context: Context,
        fileName: String,
        title: String,
        cards: List<PdfCardData>,
        branding: PdfBranding = PdfBranding.NONE
    ): Intent {
        val document = PdfDocument()
        val titleAccented = branding.template == ReportTemplate.MODERN || branding.template == ReportTemplate.BOLD
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (titleAccented) branding.accentColor else Color.BLACK; textSize = 16f; isFakeBoldText = true
        }
        val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6B7280"); textSize = 9f }
        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111827"); textSize = 12f; isFakeBoldText = true }
        val subheadlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4B5563"); textSize = 9.5f }
        val fieldLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4B5563"); textSize = 9f; isFakeBoldText = true }
        val fieldValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#111827"); textSize = 9f }
        val footerFieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9CA3AF"); textSize = 8f }
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = branding.accentColor }
        val pageFooterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#9CA3AF"); textSize = 8f }

        val contentWidth = PAGE_WIDTH - 2 * MARGIN

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

        canvas.drawText(title, MARGIN, y + 12f, titlePaint)
        y += 16f
        val generatedAt = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date())
        canvas.drawText("Total: ${cards.size}   Generated: $generatedAt", MARGIN, y + 10f, summaryPaint)
        y += 26f

        cards.forEach { card ->
            val textX = MARGIN + 14f
            val textWidth = contentWidth - 14f

            val headlineLines = wrapText(card.headline, headlinePaint, textWidth)
            val subheadlineLines = card.subheadline?.takeIf { it.isNotBlank() }?.let { wrapText(it, subheadlinePaint, textWidth) }.orEmpty()
            val fieldLines = card.fields.map { (label, value) ->
                label to wrapText(value, fieldValuePaint, (textWidth - fieldLabelPaint.measureText("$label: ")).coerceAtLeast(60f))
            }

            var blockHeight = 12f + headlineLines.size * 14f
            if (subheadlineLines.isNotEmpty()) blockHeight += 4f + subheadlineLines.size * 12f
            if (fieldLines.isNotEmpty()) {
                blockHeight += 6f
                fieldLines.forEach { (_, lines) -> blockHeight += lines.size.coerceAtLeast(1) * 13f }
            }
            if (!card.footer.isNullOrBlank()) blockHeight += 14f
            blockHeight += 10f

            checkPage(blockHeight + 14f)

            canvas.drawRect(MARGIN, y, MARGIN + 3f, y + blockHeight, barPaint)
            var cy = y + 12f
            headlineLines.forEach { line -> canvas.drawText(line, textX, cy, headlinePaint); cy += 14f }

            if (subheadlineLines.isNotEmpty()) {
                cy += 4f
                subheadlineLines.forEach { line -> canvas.drawText(line, textX, cy, subheadlinePaint); cy += 12f }
            }

            if (fieldLines.isNotEmpty()) {
                cy += 6f
                fieldLines.forEach { (label, lines) ->
                    val labelText = "$label: "
                    val labelWidth = fieldLabelPaint.measureText(labelText)
                    canvas.drawText(labelText, textX, cy, fieldLabelPaint)
                    lines.forEachIndexed { li, line ->
                        canvas.drawText(line, if (li == 0) textX + labelWidth else textX + 10f, cy, fieldValuePaint)
                        cy += 13f
                    }
                }
            }

            if (!card.footer.isNullOrBlank()) {
                cy += 2f
                canvas.drawText(card.footer, textX, cy, footerFieldPaint)
            }

            y += blockHeight + 14f
        }

        checkPage(16f)
        canvas.drawText("Generated on $generatedAt", MARGIN, y, pageFooterPaint)

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

    /** Wraps on whitespace only — same shape as every other exporter's own copy of this helper. */
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
