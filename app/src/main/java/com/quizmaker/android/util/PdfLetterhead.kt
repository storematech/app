package com.quizmaker.android.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/**
 * Draws the account's optional letterhead — logo and/or address — at the top of a report's first
 * page, then a thin divider, and returns the new content-start [y]. A no-op returning [y]
 * unchanged when [branding] is [PdfBranding.NONE], so a report from an account with nothing
 * uploaded looks exactly like it did before this existed. Every *PdfExporter calls this once,
 * right after `y = MARGIN`, before drawing its own title.
 */
object PdfLetterhead {
    private const val LOGO_MAX_HEIGHT = 42f
    private const val LOGO_MAX_WIDTH = 130f
    private const val MAX_NAME_LINES = 2
    private const val MAX_ADDRESS_LINES = 3

    fun draw(canvas: Canvas, marginLeft: Float, marginRight: Float, y: Float, branding: PdfBranding): Float {
        val logo = branding.logo
        val businessName = branding.businessName
        val address = branding.address
        if (logo == null && businessName.isNullOrBlank() && address.isNullOrBlank()) return y

        var logoBottom = y
        var textX = marginLeft
        if (logo != null) {
            val scale = LOGO_MAX_HEIGHT / logo.height.toFloat()
            val width = (logo.width * scale).coerceAtMost(LOGO_MAX_WIDTH)
            canvas.drawBitmap(
                logo,
                Rect(0, 0, logo.width, logo.height),
                RectF(marginLeft, y, marginLeft + width, y + LOGO_MAX_HEIGHT),
                null
            )
            logoBottom = y + LOGO_MAX_HEIGHT
            textX = marginLeft + width + 12f
        }

        var ty = y + 10f
        val textWidth = marginRight - textX

        if (!businessName.isNullOrBlank()) {
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#111827")
                textSize = 11f
                isFakeBoldText = true
            }
            wrapText(businessName, namePaint, textWidth, MAX_NAME_LINES).forEach { line ->
                canvas.drawText(line, textX, ty, namePaint)
                ty += 13f
            }
        }

        if (!address.isNullOrBlank()) {
            val addressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4B5563"); textSize = 8.5f }
            wrapText(address, addressPaint, textWidth, MAX_ADDRESS_LINES).forEach { line ->
                canvas.drawText(line, textX, ty, addressPaint)
                ty += 11f
            }
        }

        // MINIMAL keeps the accent color out of even this divider — a neutral hairline, same as its
        // table headers; every other template uses the accent (BOLD a touch thicker for emphasis).
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (branding.template == ReportTemplate.MINIMAL) Color.parseColor("#E5E7EB") else branding.accentColor
            strokeWidth = if (branding.template == ReportTemplate.BOLD) 2f else 1f
        }
        val blockBottom = maxOf(logoBottom, ty) + 8f
        canvas.drawLine(marginLeft, blockBottom, marginRight, blockBottom, dividerPaint)
        return blockBottom + 12f
    }

    /** Wraps on whitespace only — a word is always kept whole, never split mid-word, and any
     *  explicit line break the user typed (address multi-line, etc.) becomes an ordinary wrap
     *  point like any other space, so the block still reflows to [maxWidth] instead of running on. */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float, maxLines: Int): List<String> {
        val words = text.trim().split(Regex("\\s+"))
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
        // A very long name/address shouldn't be able to push the header height (and therefore the
        // rest of the page layout) out unpredictably — same reasoning as every other exporter's
        // own per-cell truncation.
        return lines.take(maxLines)
    }
}
