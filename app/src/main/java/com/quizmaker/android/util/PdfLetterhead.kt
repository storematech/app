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
    private const val MAX_ADDRESS_LINES = 3

    fun draw(canvas: Canvas, marginLeft: Float, marginRight: Float, y: Float, branding: PdfBranding): Float {
        val logo = branding.logo
        val address = branding.address
        if (logo == null && address.isNullOrBlank()) return y

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

        var addressBottom = y
        if (!address.isNullOrBlank()) {
            val addressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4B5563"); textSize = 8.5f }
            var ty = y + 10f
            wrapText(address, addressPaint, marginRight - textX).forEach { line ->
                canvas.drawText(line, textX, ty, addressPaint)
                ty += 11f
            }
            addressBottom = ty
        }

        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 1f }
        val blockBottom = maxOf(logoBottom, addressBottom) + 8f
        canvas.drawLine(marginLeft, blockBottom, marginRight, blockBottom, dividerPaint)
        return blockBottom + 12f
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
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
        // A very long address shouldn't be able to push the header height (and therefore the rest
        // of the page layout) out unpredictably — same reasoning as every other exporter's own
        // per-cell truncation.
        return lines.take(MAX_ADDRESS_LINES)
    }
}
