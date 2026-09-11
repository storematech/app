package com.quizmaker.android.ui.certificate

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.quizmaker.android.R

/** Landscape A4 at 72dpi — same point-based sizing convention as this app's other PDF exporters
 *  (see MasterPaperPdfExporter's PAGE_WIDTH/PAGE_HEIGHT), just swapped width/height for landscape.
 *  Shared by the on-screen Compose preview (scaled down) and CertificatePdfExporter (1:1). */
const val CERTIFICATE_WIDTH = 842f
const val CERTIFICATE_HEIGHT = 595f

/** Render-time data for a certificate — distinct from CertificateDesignDto (the persisted record):
 *  [recipientName] is never persisted (preview-only on the designer screen; the real value comes
 *  from whatever awards the certificate), and dates are pre-formatted here rather than carrying a
 *  raw timestamp + formatting logic into the drawing functions. */
data class CertificateRenderData(
    val title: String,
    val bodyText: String,
    val companyName: String,
    val signerName: String,
    val signerRole: String,
    val recipientName: String,
    val issueDateLabel: String,
    val issueDateText: String
)

// ---- Shared drawing helpers -------------------------------------------------------------------

private fun typefaceOf(context: Context, resId: Int): Typeface? =
    runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull()

private fun paint(color: Int, size: Float, tf: Typeface? = null, align: Paint.Align = Paint.Align.LEFT): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = tf
        textAlign = align
    }

private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
    if (text.isBlank()) return emptyList()
    val lines = mutableListOf<String>()
    text.split("\n").forEach { paragraph ->
        val words = paragraph.split(" ")
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
    }
    return lines
}

private fun Canvas.drawCenteredWrapped(text: String, centerX: Float, top: Float, maxWidth: Float, paint: Paint, lineHeight: Float): Float {
    var y = top
    wrapText(text, paint, maxWidth).forEach { line ->
        drawText(line, centerX, y, paint)
        y += lineHeight
    }
    return y
}

private fun Canvas.drawLeftWrapped(text: String, x: Float, top: Float, maxWidth: Float, paint: Paint, lineHeight: Float): Float {
    var y = top
    wrapText(text, paint, maxWidth).forEach { line ->
        drawText(line, x, y, paint)
        y += lineHeight
    }
    return y
}

private fun applyLetterSpacing(paint: Paint, em: Float) {
    paint.letterSpacing = em
}

/** Circular badge with a single initial letter — the logo stand-in used by Modern/Corporate when
 *  no logo image is set. */
private fun Canvas.drawInitialBadge(centerX: Float, centerY: Float, radius: Float, badgeColor: Int, initial: String, textColor: Int, tf: Typeface?) {
    val bgPaint = paint(badgeColor, 0f)
    drawCircle(centerX, centerY, radius, bgPaint)
    val textPaint = paint(textColor, radius * 1.1f, tf, Paint.Align.CENTER)
    val fm = textPaint.fontMetrics
    drawText(initial, centerX, centerY - (fm.ascent + fm.descent) / 2f, textPaint)
}

private fun Canvas.drawBitmapCentered(bitmap: Bitmap, centerX: Float, centerY: Float, maxSize: Float) {
    val scale = minOf(maxSize / bitmap.width, maxSize / bitmap.height)
    val w = bitmap.width * scale
    val h = bitmap.height * scale
    val dest = RectF(centerX - w / 2f, centerY - h / 2f, centerX + w / 2f, centerY + h / 2f)
    drawBitmap(bitmap, null, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
}

/** Logo badge clipped to a circle: the image is scaled to COVER the circle (cropped, not
 *  letterboxed) and hard-clipped to its bounds, so a rectangular/non-square logo never pokes
 *  corners out past the round badge the way plain center-fit drawing would. A white backdrop is
 *  drawn first as a safety net for logos with transparent backgrounds. */
private fun Canvas.drawBitmapInCircle(bitmap: Bitmap, centerX: Float, centerY: Float, radius: Float) {
    drawCircle(centerX, centerY, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
    val save = save()
    val clipPath = Path().apply { addCircle(centerX, centerY, radius, Path.Direction.CW) }
    clipPath(clipPath)
    val scale = maxOf(radius * 2f / bitmap.width, radius * 2f / bitmap.height)
    val w = bitmap.width * scale
    val h = bitmap.height * scale
    val dest = RectF(centerX - w / 2f, centerY - h / 2f, centerX + w / 2f, centerY + h / 2f)
    drawBitmap(bitmap, null, dest, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    restoreToCount(save)
}

// ---- Traditional ---------------------------------------------------------------------------

/** Double-line gold border, gothic title, cursive recipient name, laurel-flourish fallback logo. */
fun drawCertificateTraditional(
    context: Context,
    canvas: Canvas,
    width: Float,
    height: Float,
    data: CertificateRenderData,
    logoBitmap: Bitmap?,
    signatureBitmap: Bitmap?
) {
    val gold = Color.parseColor("#C9A227")
    canvas.drawColor(Color.WHITE)

    val outerInset = 16f
    val innerInset = 26f
    val borderPaintOuter = paint(gold, 0f).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f }
    val borderPaintInner = paint(gold, 0f).apply { style = Paint.Style.STROKE; strokeWidth = 1f }
    canvas.drawRect(outerInset, outerInset, width - outerInset, height - outerInset, borderPaintOuter)
    canvas.drawRect(innerInset, innerInset, width - innerInset, height - innerInset, borderPaintInner)

    // Small diamond corner accents, top/bottom center.
    val diamondPaint = paint(gold, 0f).apply { style = Paint.Style.FILL }
    listOf(outerInset, height - outerInset).forEach { y ->
        val path = Path().apply {
            moveTo(width / 2f, y - 6f)
            lineTo(width / 2f + 6f, y)
            lineTo(width / 2f, y + 6f)
            lineTo(width / 2f - 6f, y)
            close()
        }
        canvas.drawPath(path, diamondPaint)
    }

    val centerX = width / 2f
    var y = 70f

    if (logoBitmap != null) {
        canvas.drawBitmapCentered(logoBitmap, centerX, y, 56f)
        y += 44f
    } else {
        // Simple laurel-ish flourish: a couple of curved/angled short strokes either side of center.
        val flourishPaint = paint(gold, 0f).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f }
        val leaf = Path().apply {
            moveTo(centerX - 30f, y)
            quadTo(centerX - 14f, y - 14f, centerX - 4f, y)
            quadTo(centerX - 14f, y + 6f, centerX - 30f, y)
        }
        canvas.drawPath(leaf, flourishPaint)
        val leafMirror = Path().apply {
            moveTo(centerX + 30f, y)
            quadTo(centerX + 14f, y - 14f, centerX + 4f, y)
            quadTo(centerX + 14f, y + 6f, centerX + 30f, y)
        }
        canvas.drawPath(leafMirror, flourishPaint)
        y += 30f
    }

    val companyPaint = paint(Color.parseColor("#999999"), 13f, tf = null, align = Paint.Align.CENTER).apply {
        applyLetterSpacing(this, 0.15f)
    }
    canvas.drawText(data.companyName.uppercase(), centerX, y, companyPaint)
    y += 34f

    val titleTf = typefaceOf(context, R.font.unifraktur_maguntia_regular)
    val titlePaint = paint(Color.parseColor("#111111"), 40f, titleTf, Paint.Align.CENTER)
    val nameTf = typefaceOf(context, R.font.dancing_script)
    val bodyTf = typefaceOf(context, R.font.eb_garamond_italic)
    val bodyPaint = paint(Color.parseColor("#555555"), 13f, bodyTf, Paint.Align.CENTER)

    // Vertically center the [title, name, body] block in the space between the
    // header (logo/company) and the footer, instead of always hugging the top —
    // short titles/body text otherwise leave a large empty gap above the footer.
    val titleLines = wrapText(data.title, titlePaint, width - 220f).size
    val bodyLines = wrapText(data.bodyText, bodyPaint, width - 260f).size
    val blockNaturalHeight = titleLines * 46f + 62f + bodyLines * 18f
    val footerZoneTop = height - 68f - 45f
    val offset = ((footerZoneTop - y - blockNaturalHeight) / 2f).coerceAtLeast(0f)
    y += offset

    y = canvas.drawCenteredWrapped(data.title, centerX, y, width - 220f, titlePaint, 46f)
    y += 20f

    val namePaint = paint(Color.parseColor("#222222"), 34f, nameTf, Paint.Align.CENTER)
    canvas.drawText(data.recipientName, centerX, y, namePaint)
    y += 12f
    val underlinePaint = paint(Color.parseColor("#BBBBBB"), 0f).apply { strokeWidth = 1f }
    canvas.drawLine(centerX - 140f, y, centerX + 140f, y, underlinePaint)
    y += 30f

    canvas.drawCenteredWrapped(data.bodyText, centerX, y, width - 260f, bodyPaint, 18f)

    // Footer row.
    val footerY = height - 68f
    val leftX = innerInset + 40f
    if (signatureBitmap != null) {
        val scale = minOf(70f / signatureBitmap.width, 28f / signatureBitmap.height)
        val w = signatureBitmap.width * scale
        val h = signatureBitmap.height * scale
        canvas.drawBitmap(signatureBitmap, null, RectF(leftX, footerY - h - 6f, leftX + w, footerY - 6f), Paint(Paint.ANTI_ALIAS_FLAG))
    }
    val rulePaint = paint(Color.parseColor("#333333"), 0f).apply { strokeWidth = 1f }
    canvas.drawLine(leftX, footerY, leftX + 140f, footerY, rulePaint)
    val signerNamePaint = paint(Color.parseColor("#333333"), 11f).apply { isFakeBoldText = true }
    canvas.drawText(data.signerName, leftX, footerY + 16f, signerNamePaint)
    val signerRolePaint = paint(Color.parseColor("#999999"), 9f)
    canvas.drawText(data.signerRole, leftX, footerY + 28f, signerRolePaint)

    val rightX = width - innerInset - 40f
    val dateLabelPaint = paint(Color.parseColor("#BBBBBB"), 8f, align = Paint.Align.RIGHT).apply { applyLetterSpacing(this, 0.1f) }
    canvas.drawText(data.issueDateLabel.uppercase(), rightX, footerY - 16f, dateLabelPaint)
    val dateTf = typefaceOf(context, R.font.eb_garamond_italic)
    val datePaint = paint(Color.parseColor("#444444"), 11f, dateTf, Paint.Align.RIGHT)
    canvas.drawText(data.issueDateText, rightX, footerY, datePaint)
}

// ---- Modern ---------------------------------------------------------------------------------

/** Asymmetric layout: a solid indigo block down the left ~110pt, everything else left-aligned on white. */
fun drawCertificateModern(
    context: Context,
    canvas: Canvas,
    width: Float,
    height: Float,
    data: CertificateRenderData,
    logoBitmap: Bitmap?,
    signatureBitmap: Bitmap?
) {
    val brandIndigo = Color.parseColor("#2563EB")
    canvas.drawColor(Color.WHITE)

    val blockWidth = 110f
    val blockPaint = paint(brandIndigo, 0f)
    canvas.drawRect(0f, 0f, blockWidth, height, blockPaint)

    val poppinsBold = typefaceOf(context, R.font.poppins_bold)
    val poppinsSemibold = typefaceOf(context, R.font.poppins_semibold)
    val poppinsRegular = typefaceOf(context, R.font.poppins_regular)

    val badgeCenterX = blockWidth / 2f
    if (logoBitmap != null) {
        canvas.drawBitmapInCircle(logoBitmap, badgeCenterX, height / 2f - 40f, 34f)
    } else {
        canvas.drawInitialBadge(
            badgeCenterX, height / 2f - 40f, 34f,
            badgeColor = Color.WHITE,
            initial = data.companyName.trim().firstOrNull()?.uppercase() ?: "?",
            textColor = brandIndigo,
            tf = poppinsBold
        )
    }
    val companyBlockPaint = paint(Color.WHITE, 11f, poppinsSemibold, Paint.Align.CENTER).apply { applyLetterSpacing(this, 0.08f) }
    canvas.drawCenteredWrapped(data.companyName.uppercase(), badgeCenterX, height - 50f, blockWidth - 20f, companyBlockPaint, 14f)

    val contentLeft = blockWidth + 56f
    val contentWidth = width - contentLeft - 56f
    var y = 110f

    val titlePaint = paint(Color.parseColor("#0F172A"), 34f, poppinsBold)
    val bodyPaint = paint(Color.parseColor("#475569"), 13f, poppinsRegular)

    // Vertically center [title, name, body] between the fixed top start and the
    // footer, instead of always hugging the top.
    val titleLines = wrapText(data.title, titlePaint, contentWidth).size
    val bodyLines = wrapText(data.bodyText, bodyPaint, contentWidth).size
    val blockNaturalHeight = titleLines * 38f + 80f + bodyLines * 18f
    val footerZoneTop = height - 60f - 45f
    val offset = ((footerZoneTop - y - blockNaturalHeight) / 2f).coerceAtLeast(0f)
    y += offset

    y = canvas.drawLeftWrapped(data.title, contentLeft, y, contentWidth, titlePaint, 38f)
    y += 6f
    val underlinePaint = paint(brandIndigo, 0f).apply { strokeWidth = 2f }
    canvas.drawLine(contentLeft, y, contentLeft + 160f, y, underlinePaint)
    y += 40f

    val namePaint = paint(Color.parseColor("#0F172A"), 28f, poppinsSemibold)
    canvas.drawText(data.recipientName, contentLeft, y, namePaint)
    y += 34f

    y = canvas.drawLeftWrapped(data.bodyText, contentLeft, y, contentWidth, bodyPaint, 18f)

    // Footer, two columns, left-aligned.
    val footerY = height - 60f
    if (signatureBitmap != null) {
        val scale = minOf(80f / signatureBitmap.width, 30f / signatureBitmap.height)
        val w = signatureBitmap.width * scale
        val h = signatureBitmap.height * scale
        canvas.drawBitmap(signatureBitmap, null, RectF(contentLeft, footerY - h - 6f, contentLeft + w, footerY - 6f), Paint(Paint.ANTI_ALIAS_FLAG))
    }
    val rulePaint = paint(Color.parseColor("#CBD5E1"), 0f).apply { strokeWidth = 1f }
    canvas.drawLine(contentLeft, footerY, contentLeft + 150f, footerY, rulePaint)
    val signerNamePaint = paint(Color.parseColor("#0F172A"), 11f, poppinsSemibold)
    canvas.drawText(data.signerName, contentLeft, footerY + 16f, signerNamePaint)
    val signerRolePaint = paint(Color.parseColor("#64748B"), 9f, poppinsRegular)
    canvas.drawText(data.signerRole, contentLeft, footerY + 28f, signerRolePaint)

    val dateColX = contentLeft + 260f
    val dateLabelPaint = paint(Color.parseColor("#94A3B8"), 8f, poppinsSemibold).apply { applyLetterSpacing(this, 0.08f) }
    canvas.drawText(data.issueDateLabel.uppercase(), dateColX, footerY - 4f, dateLabelPaint)
    val datePaint = paint(Color.parseColor("#0F172A"), 12f, poppinsRegular)
    canvas.drawText(data.issueDateText, dateColX, footerY + 14f, datePaint)
}

// ---- Simple -------------------------------------------------------------------------------

/** Near-monochrome, minimal — no border (a single thin inset rule), lots of whitespace, EB Garamond throughout. */
fun drawCertificateSimple(
    context: Context,
    canvas: Canvas,
    width: Float,
    height: Float,
    data: CertificateRenderData,
    logoBitmap: Bitmap?,
    signatureBitmap: Bitmap?
) {
    canvas.drawColor(Color.WHITE)

    val insetRulePaint = paint(Color.parseColor("#E5E7EB"), 0f).apply { style = Paint.Style.STROKE; strokeWidth = 1f }
    canvas.drawRect(40f, 40f, width - 40f, height - 40f, insetRulePaint)

    val garamond = typefaceOf(context, R.font.eb_garamond)
    val garamondItalic = typefaceOf(context, R.font.eb_garamond_italic)
    val centerX = width / 2f
    var y = 110f

    val titlePaint = paint(Color.parseColor("#111111"), 26f, garamond, Paint.Align.CENTER).apply { applyLetterSpacing(this, 0.14f) }
    val bodyPaint = paint(Color.parseColor("#4B4B4B"), 12.5f, garamondItalic, Paint.Align.CENTER)

    // Vertically center the whole block (header mark, title, name, body) in the
    // space above the footer instead of always hugging the top.
    val headerGap = if (logoBitmap != null) 40f else 30f
    val titleLines = wrapText(data.title.uppercase(), titlePaint, width - 280f).size
    val bodyLines = wrapText(data.bodyText, bodyPaint, width - 320f).size
    val blockNaturalHeight = headerGap + titleLines * 32f + 112f + bodyLines * 20f
    val footerZoneTop = height - 80f - 45f
    val offset = ((footerZoneTop - y - blockNaturalHeight) / 2f).coerceAtLeast(0f)
    y += offset

    if (logoBitmap != null) {
        canvas.drawBitmapCentered(logoBitmap, centerX, y, 44f)
        y += 40f
    } else {
        val companyPaint = paint(Color.parseColor("#111111"), 12f, garamond, Paint.Align.CENTER).apply { applyLetterSpacing(this, 0.2f) }
        canvas.drawText(data.companyName.uppercase(), centerX, y, companyPaint)
        y += 30f
    }

    y = canvas.drawCenteredWrapped(data.title.uppercase(), centerX, y, width - 280f, titlePaint, 32f)
    y += 30f

    val ruleWidth = 200f
    val rulePaint = paint(Color.parseColor("#111111"), 0f).apply { strokeWidth = 0.75f }
    canvas.drawLine(centerX - ruleWidth / 2f, y, centerX + ruleWidth / 2f, y, rulePaint)
    y += 30f

    val namePaint = paint(Color.parseColor("#111111"), 26f, garamond, Paint.Align.CENTER)
    canvas.drawText(data.recipientName, centerX, y, namePaint)
    y += 20f
    canvas.drawLine(centerX - ruleWidth / 2f, y, centerX + ruleWidth / 2f, y, rulePaint)
    y += 32f

    canvas.drawCenteredWrapped(data.bodyText, centerX, y, width - 320f, bodyPaint, 20f)

    // Minimal footer — signer name + thin rule centered under it, date in caps beneath.
    val footerY = height - 80f
    if (signatureBitmap != null) {
        val scale = minOf(70f / signatureBitmap.width, 24f / signatureBitmap.height)
        val w = signatureBitmap.width * scale
        val h = signatureBitmap.height * scale
        canvas.drawBitmap(signatureBitmap, null, RectF(centerX - w / 2f, footerY - h - 20f, centerX + w / 2f, footerY - 20f), Paint(Paint.ANTI_ALIAS_FLAG))
    }
    canvas.drawLine(centerX - 90f, footerY, centerX + 90f, footerY, rulePaint)
    val signerNamePaint = paint(Color.parseColor("#111111"), 11f, garamond, Paint.Align.CENTER)
    canvas.drawText(data.signerName, centerX, footerY + 16f, signerNamePaint)
    val signerRolePaint = paint(Color.parseColor("#8A8A8A"), 9f, garamondItalic, Paint.Align.CENTER)
    canvas.drawText(data.signerRole, centerX, footerY + 28f, signerRolePaint)

    val datePaint = paint(Color.parseColor("#8A8A8A"), 9f, garamond, Paint.Align.CENTER).apply { applyLetterSpacing(this, 0.1f) }
    canvas.drawText("${data.issueDateLabel.uppercase()}: ${data.issueDateText}", centerX, footerY + 46f, datePaint)
}

// ---- Corporate ----------------------------------------------------------------------------

/** Structured/formal: full-width navy header band, formal two-column footer with a vertical rule, echoed navy strip at the bottom. */
fun drawCertificateCorporate(
    context: Context,
    canvas: Canvas,
    width: Float,
    height: Float,
    data: CertificateRenderData,
    logoBitmap: Bitmap?,
    signatureBitmap: Bitmap?
) {
    val navy = Color.parseColor("#1E293B")
    canvas.drawColor(Color.WHITE)

    val headerHeight = 70f
    canvas.drawRect(0f, 0f, width, headerHeight, paint(navy, 0f))
    // Echo the navy band as a thin strip at the very bottom for symmetry.
    canvas.drawRect(0f, height - 6f, width, height, paint(navy, 0f))

    val garamond = typefaceOf(context, R.font.eb_garamond)
    val badgeCenterX = 70f
    val badgeCenterY = headerHeight / 2f
    if (logoBitmap != null) {
        canvas.drawBitmapInCircle(logoBitmap, badgeCenterX, badgeCenterY, 22f)
    } else {
        canvas.drawInitialBadge(
            badgeCenterX, badgeCenterY, 22f,
            badgeColor = Color.WHITE,
            initial = data.companyName.trim().firstOrNull()?.uppercase() ?: "?",
            textColor = navy,
            tf = garamond
        )
    }
    val companyPaint = paint(Color.WHITE, 15f, garamond).apply { applyLetterSpacing(this, 0.1f) }
    val fm = companyPaint.fontMetrics
    canvas.drawText(data.companyName.uppercase(), badgeCenterX + 40f, badgeCenterY - (fm.ascent + fm.descent) / 2f, companyPaint)

    val centerX = width / 2f
    var y = headerHeight + 66f

    val titlePaint = paint(navy, 30f, garamond, Paint.Align.CENTER)
    val bodyPaint = paint(Color.parseColor("#374151"), 13f, garamond, Paint.Align.CENTER)

    // Vertically center [title, name, body] between the header band and the
    // footer, instead of always hugging the top.
    val titleLines = wrapText(data.title, titlePaint, width - 220f).size
    val bodyLines = wrapText(data.bodyText, bodyPaint, width - 260f).size
    val blockNaturalHeight = titleLines * 34f + 52f + bodyLines * 18f
    val footerZoneTop = height - 60f - 45f
    val offset = ((footerZoneTop - y - blockNaturalHeight) / 2f).coerceAtLeast(0f)
    y += offset

    y = canvas.drawCenteredWrapped(data.title, centerX, y, width - 220f, titlePaint, 34f)
    y += 22f

    val namePaint = paint(Color.parseColor("#111111"), 26f, garamond, Paint.Align.CENTER)
    canvas.drawText(data.recipientName, centerX, y, namePaint)
    y += 30f

    canvas.drawCenteredWrapped(data.bodyText, centerX, y, width - 260f, bodyPaint, 18f)

    // Formal two-column footer with a thin vertical rule between the columns.
    val footerY = height - 60f
    val leftX = 90f
    val dividerX = width / 2f
    val rightX = width - 90f

    if (signatureBitmap != null) {
        val scale = minOf(70f / signatureBitmap.width, 26f / signatureBitmap.height)
        val w = signatureBitmap.width * scale
        val h = signatureBitmap.height * scale
        canvas.drawBitmap(signatureBitmap, null, RectF(leftX, footerY - h - 6f, leftX + w, footerY - 6f), Paint(Paint.ANTI_ALIAS_FLAG))
    }
    val rulePaint = paint(Color.parseColor("#475569"), 0f).apply { strokeWidth = 1f }
    canvas.drawLine(leftX, footerY, leftX + 140f, footerY, rulePaint)
    val signerNamePaint = paint(navy, 11f, garamond).apply { isFakeBoldText = true }
    canvas.drawText(data.signerName, leftX, footerY + 16f, signerNamePaint)
    val signerRolePaint = paint(Color.parseColor("#64748B"), 9f, garamond)
    canvas.drawText(data.signerRole, leftX, footerY + 28f, signerRolePaint)

    val verticalRulePaint = paint(Color.parseColor("#475569"), 0f).apply { strokeWidth = 1f }
    canvas.drawLine(dividerX, footerY - 32f, dividerX, footerY + 28f, verticalRulePaint)

    val dateLabelPaint = paint(Color.parseColor("#64748B"), 8f, garamond, Paint.Align.RIGHT).apply { applyLetterSpacing(this, 0.1f) }
    canvas.drawText(data.issueDateLabel.uppercase(), rightX, footerY - 4f, dateLabelPaint)
    val datePaint = paint(navy, 12f, garamond, Paint.Align.RIGHT)
    canvas.drawText(data.issueDateText, rightX, footerY + 14f, datePaint)
}

// ---- Elegant --------------------------------------------------------------------------------

/** Ivory background, thin rose-gold double rule with a small diamond flourish at all four corners
 *  (Traditional only marks top/bottom-center), EB Garamond throughout, centered — a softer, warmer
 *  take on Traditional's gold-on-white. */
fun drawCertificateElegant(
    context: Context,
    canvas: Canvas,
    width: Float,
    height: Float,
    data: CertificateRenderData,
    logoBitmap: Bitmap?,
    signatureBitmap: Bitmap?
) {
    val ivory = Color.parseColor("#FDFBF5")
    val rose = Color.parseColor("#A6677A")
    canvas.drawColor(ivory)

    val outerInset = 18f
    val innerInset = 27f
    val outerBorderPaint = paint(rose, 0f).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f }
    val innerBorderPaint = paint(rose, 0f).apply { style = Paint.Style.STROKE; strokeWidth = 0.75f }
    canvas.drawRect(outerInset, outerInset, width - outerInset, height - outerInset, outerBorderPaint)
    canvas.drawRect(innerInset, innerInset, width - innerInset, height - innerInset, innerBorderPaint)

    val diamondPaint = paint(rose, 0f).apply { style = Paint.Style.FILL }
    val corners = listOf(
        outerInset to outerInset,
        width - outerInset to outerInset,
        outerInset to height - outerInset,
        width - outerInset to height - outerInset
    )
    corners.forEach { (cx, cy) ->
        val path = Path().apply {
            moveTo(cx, cy - 7f)
            lineTo(cx + 7f, cy)
            lineTo(cx, cy + 7f)
            lineTo(cx - 7f, cy)
            close()
        }
        canvas.drawPath(path, diamondPaint)
    }

    val garamond = typefaceOf(context, R.font.eb_garamond)
    val garamondItalic = typefaceOf(context, R.font.eb_garamond_italic)
    val centerX = width / 2f
    var y = 78f

    if (logoBitmap != null) {
        canvas.drawBitmapInCircle(logoBitmap, centerX, y + 8f, 30f)
        y += 56f
    } else {
        val companyPaint = paint(Color.parseColor("#8A6B72"), 12f, garamondItalic, Paint.Align.CENTER).apply {
            applyLetterSpacing(this, 0.12f)
        }
        canvas.drawText(data.companyName.uppercase(), centerX, y, companyPaint)
        y += 30f
    }

    val titlePaint = paint(Color.parseColor("#3A2C30"), 34f, garamond, Paint.Align.CENTER).apply {
        applyLetterSpacing(this, 0.04f)
    }
    val nameTf = typefaceOf(context, R.font.dancing_script)
    val bodyPaint = paint(Color.parseColor("#6B5A5E"), 12.5f, garamondItalic, Paint.Align.CENTER)

    // Vertically center [title, name, body] between the header and the footer.
    val titleLines = wrapText(data.title, titlePaint, width - 240f).size
    val bodyLines = wrapText(data.bodyText, bodyPaint, width - 280f).size
    val blockNaturalHeight = titleLines * 40f + 66f + bodyLines * 18f
    val footerZoneTop = height - 74f - 45f
    val offset = ((footerZoneTop - y - blockNaturalHeight) / 2f).coerceAtLeast(0f)
    y += offset

    y = canvas.drawCenteredWrapped(data.title, centerX, y, width - 240f, titlePaint, 40f)
    y += 20f

    val namePaint = paint(Color.parseColor("#3A2C30"), 32f, nameTf, Paint.Align.CENTER)
    canvas.drawText(data.recipientName, centerX, y, namePaint)
    y += 12f
    val rulePaint = paint(rose, 0f).apply { strokeWidth = 1f }
    canvas.drawLine(centerX - 130f, y, centerX + 130f, y, rulePaint)
    y += 34f

    canvas.drawCenteredWrapped(data.bodyText, centerX, y, width - 280f, bodyPaint, 18f)

    // Footer.
    val footerY = height - 74f
    val leftX = innerInset + 42f
    if (signatureBitmap != null) {
        val scale = minOf(70f / signatureBitmap.width, 26f / signatureBitmap.height)
        val w = signatureBitmap.width * scale
        val h = signatureBitmap.height * scale
        canvas.drawBitmap(signatureBitmap, null, RectF(leftX, footerY - h - 6f, leftX + w, footerY - 6f), Paint(Paint.ANTI_ALIAS_FLAG))
    }
    canvas.drawLine(leftX, footerY, leftX + 130f, footerY, rulePaint)
    val signerNamePaint = paint(Color.parseColor("#3A2C30"), 11f, garamond).apply { isFakeBoldText = true }
    canvas.drawText(data.signerName, leftX, footerY + 16f, signerNamePaint)
    val signerRolePaint = paint(Color.parseColor("#8A6B72"), 9f, garamondItalic)
    canvas.drawText(data.signerRole, leftX, footerY + 28f, signerRolePaint)

    val rightX = width - innerInset - 42f
    val dateLabelPaint = paint(Color.parseColor("#B79AA0"), 8f, garamond, Paint.Align.RIGHT).apply { applyLetterSpacing(this, 0.1f) }
    canvas.drawText(data.issueDateLabel.uppercase(), rightX, footerY - 16f, dateLabelPaint)
    val datePaint = paint(Color.parseColor("#3A2C30"), 11f, garamondItalic, Paint.Align.RIGHT)
    canvas.drawText(data.issueDateText, rightX, footerY, datePaint)
}

// ---- Ribbon ---------------------------------------------------------------------------------

/** Bold, celebratory "award seal" — a medal badge with ribbon tails at the top center, gold and
 *  crimson, big centered Poppins type. Distinct from every other template's quieter badge
 *  treatment — this one is meant to look like a medal, not a logo frame. */
fun drawCertificateRibbon(
    context: Context,
    canvas: Canvas,
    width: Float,
    height: Float,
    data: CertificateRenderData,
    logoBitmap: Bitmap?,
    signatureBitmap: Bitmap?
) {
    val gold = Color.parseColor("#D9A72C")
    val crimson = Color.parseColor("#B91C1C")
    canvas.drawColor(Color.WHITE)

    val outerInset = 16f
    val borderPaint = paint(Color.parseColor("#EDE0C0"), 0f).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
    canvas.drawRect(outerInset, outerInset, width - outerInset, height - outerInset, borderPaint)

    val centerX = width / 2f
    val medalCenterY = 62f
    val medalRadius = 32f

    // Ribbon tails — a simple pennant-shaped triangle hanging from each side of the medal.
    val ribbonPaint = paint(crimson, 0f).apply { style = Paint.Style.FILL }
    listOf(-1f, 1f).forEach { side ->
        val path = Path().apply {
            moveTo(centerX + side * 4f, medalCenterY + medalRadius - 6f)
            lineTo(centerX + side * 22f, medalCenterY + medalRadius + 44f)
            lineTo(centerX + side * 10f, medalCenterY + medalRadius + 30f)
            close()
        }
        canvas.drawPath(path, ribbonPaint)
    }

    val poppinsExtraBold = typefaceOf(context, R.font.poppins_extrabold)
    val poppinsBold = typefaceOf(context, R.font.poppins_bold)
    val poppinsSemibold = typefaceOf(context, R.font.poppins_semibold)
    val poppinsRegular = typefaceOf(context, R.font.poppins_regular)

    if (logoBitmap != null) {
        canvas.drawBitmapInCircle(logoBitmap, centerX, medalCenterY, medalRadius)
    } else {
        canvas.drawInitialBadge(
            centerX, medalCenterY, medalRadius,
            badgeColor = gold,
            initial = data.companyName.trim().firstOrNull()?.uppercase() ?: "?",
            textColor = Color.WHITE,
            tf = poppinsExtraBold
        )
    }

    var y = medalCenterY + medalRadius + 60f

    val titlePaint = paint(Color.parseColor("#1F2937"), 32f, poppinsExtraBold, Paint.Align.CENTER)
    val bodyPaint = paint(Color.parseColor("#4B5563"), 12.5f, poppinsRegular, Paint.Align.CENTER)

    val titleLines = wrapText(data.title, titlePaint, width - 220f).size
    val bodyLines = wrapText(data.bodyText, bodyPaint, width - 270f).size
    val blockNaturalHeight = titleLines * 38f + 60f + bodyLines * 18f
    val footerZoneTop = height - 70f - 45f
    val offset = ((footerZoneTop - y - blockNaturalHeight) / 2f).coerceAtLeast(0f)
    y += offset

    y = canvas.drawCenteredWrapped(data.title, centerX, y, width - 220f, titlePaint, 38f)
    y += 18f

    val namePaint = paint(crimson, 28f, poppinsBold, Paint.Align.CENTER)
    canvas.drawText(data.recipientName, centerX, y, namePaint)
    y += 32f

    canvas.drawCenteredWrapped(data.bodyText, centerX, y, width - 270f, bodyPaint, 18f)

    // Footer — centered, gold rule.
    val footerY = height - 70f
    if (signatureBitmap != null) {
        val scale = minOf(70f / signatureBitmap.width, 26f / signatureBitmap.height)
        val w = signatureBitmap.width * scale
        val h = signatureBitmap.height * scale
        canvas.drawBitmap(signatureBitmap, null, RectF(centerX - w / 2f, footerY - h - 18f, centerX + w / 2f, footerY - 18f), Paint(Paint.ANTI_ALIAS_FLAG))
    }
    val rulePaint = paint(gold, 0f).apply { strokeWidth = 1.5f }
    canvas.drawLine(centerX - 90f, footerY, centerX + 90f, footerY, rulePaint)
    val signerNamePaint = paint(Color.parseColor("#1F2937"), 11f, poppinsSemibold, Paint.Align.CENTER)
    canvas.drawText(data.signerName, centerX, footerY + 16f, signerNamePaint)
    val signerRolePaint = paint(Color.parseColor("#6B7280"), 9f, poppinsRegular, Paint.Align.CENTER)
    canvas.drawText(data.signerRole, centerX, footerY + 28f, signerRolePaint)
    val datePaint = paint(Color.parseColor("#9CA3AF"), 9f, poppinsRegular, Paint.Align.CENTER).apply { applyLetterSpacing(this, 0.08f) }
    canvas.drawText("${data.issueDateLabel.uppercase()}: ${data.issueDateText}", centerX, footerY + 44f, datePaint)
}

// ---- Geometric ------------------------------------------------------------------------------

/** White canvas, a bold diagonal triangular color-block accent in the top-right corner,
 *  left-aligned Poppins throughout — a fresh, contemporary composition distinct from Modern's
 *  vertical side band. */
fun drawCertificateGeometric(
    context: Context,
    canvas: Canvas,
    width: Float,
    height: Float,
    data: CertificateRenderData,
    logoBitmap: Bitmap?,
    signatureBitmap: Bitmap?
) {
    val violet = Color.parseColor("#7C3AED")
    val violetLight = Color.parseColor("#F3E8FF")
    canvas.drawColor(Color.WHITE)

    val bigTriangle = Path().apply {
        moveTo(width, 0f)
        lineTo(width - 190f, 0f)
        lineTo(width, 150f)
        close()
    }
    canvas.drawPath(bigTriangle, paint(violet, 0f))
    val smallTriangle = Path().apply {
        moveTo(width, 0f)
        lineTo(width - 260f, 0f)
        lineTo(width, 60f)
        close()
    }
    canvas.drawPath(smallTriangle, paint(violetLight, 0f))

    val leftX = 56f
    val poppinsBold = typefaceOf(context, R.font.poppins_bold)
    val poppinsSemibold = typefaceOf(context, R.font.poppins_semibold)
    val poppinsRegular = typefaceOf(context, R.font.poppins_regular)

    var y = 70f
    if (logoBitmap != null) {
        canvas.drawBitmapInCircle(logoBitmap, leftX + 24f, y + 4f, 24f)
    } else {
        canvas.drawInitialBadge(
            leftX + 24f, y + 4f, 24f,
            badgeColor = violet,
            initial = data.companyName.trim().firstOrNull()?.uppercase() ?: "?",
            textColor = Color.WHITE,
            tf = poppinsBold
        )
    }
    val companyPaint = paint(Color.parseColor("#374151"), 12f, poppinsSemibold).apply { applyLetterSpacing(this, 0.05f) }
    canvas.drawText(data.companyName.uppercase(), leftX + 58f, y + 8f, companyPaint)
    y += 74f

    val titlePaint = paint(Color.parseColor("#0F172A"), 32f, poppinsBold)
    val bodyPaint = paint(Color.parseColor("#4B5563"), 13f, poppinsRegular)
    val contentWidth = width - leftX - 70f

    val titleLines = wrapText(data.title, titlePaint, contentWidth).size
    val bodyLines = wrapText(data.bodyText, bodyPaint, contentWidth).size
    val blockNaturalHeight = titleLines * 37f + 78f + bodyLines * 18f
    val footerZoneTop = height - 62f - 45f
    val offset = ((footerZoneTop - y - blockNaturalHeight) / 2f).coerceAtLeast(0f)
    y += offset

    y = canvas.drawLeftWrapped(data.title, leftX, y, contentWidth, titlePaint, 37f)
    y += 8f
    val underlinePaint = paint(violet, 0f).apply { strokeWidth = 3f }
    canvas.drawLine(leftX, y, leftX + 70f, y, underlinePaint)
    y += 38f

    val nameTf = typefaceOf(context, R.font.poppins_semibold)
    val namePaint = paint(Color.parseColor("#0F172A"), 26f, nameTf)
    canvas.drawText(data.recipientName, leftX, y, namePaint)
    y += 32f

    y = canvas.drawLeftWrapped(data.bodyText, leftX, y, contentWidth, bodyPaint, 18f)

    val footerY = height - 62f
    if (signatureBitmap != null) {
        val scale = minOf(80f / signatureBitmap.width, 28f / signatureBitmap.height)
        val w = signatureBitmap.width * scale
        val h = signatureBitmap.height * scale
        canvas.drawBitmap(signatureBitmap, null, RectF(leftX, footerY - h - 6f, leftX + w, footerY - 6f), Paint(Paint.ANTI_ALIAS_FLAG))
    }
    val rulePaint = paint(Color.parseColor("#E9D5FF"), 0f).apply { strokeWidth = 1f }
    canvas.drawLine(leftX, footerY, leftX + 150f, footerY, rulePaint)
    val signerNamePaint = paint(Color.parseColor("#0F172A"), 11f, poppinsSemibold)
    canvas.drawText(data.signerName, leftX, footerY + 16f, signerNamePaint)
    val signerRolePaint = paint(Color.parseColor("#6B7280"), 9f, poppinsRegular)
    canvas.drawText(data.signerRole, leftX, footerY + 28f, signerRolePaint)

    val rightX = width - 56f
    val dateLabelPaint = paint(violet, 8f, poppinsSemibold, Paint.Align.RIGHT).apply { applyLetterSpacing(this, 0.08f) }
    canvas.drawText(data.issueDateLabel.uppercase(), rightX, footerY - 4f, dateLabelPaint)
    val datePaint = paint(Color.parseColor("#0F172A"), 12f, poppinsRegular, Paint.Align.RIGHT)
    canvas.drawText(data.issueDateText, rightX, footerY + 14f, datePaint)
}

// ---- Dark Premium ---------------------------------------------------------------------------

/** Full charcoal-navy background, thin gold border and rules, gold title, white/light-gray body —
 *  the one dark-background template in the set, for a high-contrast "gala/premium" look. */
fun drawCertificateDark(
    context: Context,
    canvas: Canvas,
    width: Float,
    height: Float,
    data: CertificateRenderData,
    logoBitmap: Bitmap?,
    signatureBitmap: Bitmap?
) {
    val charcoal = Color.parseColor("#12141C")
    val gold = Color.parseColor("#D4AF37")
    canvas.drawColor(charcoal)

    val outerInset = 18f
    val innerInset = 27f
    val outerBorderPaint = paint(gold, 0f).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f }
    val innerBorderPaint = paint(gold, 0f).apply { style = Paint.Style.STROKE; strokeWidth = 0.6f }
    canvas.drawRect(outerInset, outerInset, width - outerInset, height - outerInset, outerBorderPaint)
    canvas.drawRect(innerInset, innerInset, width - innerInset, height - innerInset, innerBorderPaint)

    val garamond = typefaceOf(context, R.font.eb_garamond)
    val garamondItalic = typefaceOf(context, R.font.eb_garamond_italic)
    val centerX = width / 2f
    var y = 76f

    if (logoBitmap != null) {
        canvas.drawBitmapInCircle(logoBitmap, centerX, y + 4f, 32f)
        y += 58f
    } else {
        val companyPaint = paint(gold, 12f, garamond, Paint.Align.CENTER).apply { applyLetterSpacing(this, 0.2f) }
        canvas.drawText(data.companyName.uppercase(), centerX, y, companyPaint)
        y += 32f
    }

    val titlePaint = paint(gold, 34f, garamond, Paint.Align.CENTER).apply { applyLetterSpacing(this, 0.03f) }
    val bodyPaint = paint(Color.parseColor("#C7C7CE"), 13f, garamondItalic, Paint.Align.CENTER)

    val titleLines = wrapText(data.title, titlePaint, width - 240f).size
    val bodyLines = wrapText(data.bodyText, bodyPaint, width - 280f).size
    val blockNaturalHeight = titleLines * 42f + 66f + bodyLines * 18f
    val footerZoneTop = height - 72f - 45f
    val offset = ((footerZoneTop - y - blockNaturalHeight) / 2f).coerceAtLeast(0f)
    y += offset

    y = canvas.drawCenteredWrapped(data.title, centerX, y, width - 240f, titlePaint, 42f)
    y += 20f

    val namePaint = paint(Color.WHITE, 30f, garamond, Paint.Align.CENTER)
    canvas.drawText(data.recipientName, centerX, y, namePaint)
    y += 12f
    val rulePaint = paint(gold, 0f).apply { strokeWidth = 1f }
    canvas.drawLine(centerX - 130f, y, centerX + 130f, y, rulePaint)
    y += 32f

    canvas.drawCenteredWrapped(data.bodyText, centerX, y, width - 280f, bodyPaint, 18f)

    val footerY = height - 72f
    val leftX = innerInset + 42f
    if (signatureBitmap != null) {
        val scale = minOf(70f / signatureBitmap.width, 26f / signatureBitmap.height)
        val w = signatureBitmap.width * scale
        val h = signatureBitmap.height * scale
        canvas.drawBitmap(signatureBitmap, null, RectF(leftX, footerY - h - 6f, leftX + w, footerY - 6f), Paint(Paint.ANTI_ALIAS_FLAG))
    }
    canvas.drawLine(leftX, footerY, leftX + 130f, footerY, rulePaint)
    val signerNamePaint = paint(Color.WHITE, 11f, garamond).apply { isFakeBoldText = true }
    canvas.drawText(data.signerName, leftX, footerY + 16f, signerNamePaint)
    val signerRolePaint = paint(Color.parseColor("#9C9CA8"), 9f, garamondItalic)
    canvas.drawText(data.signerRole, leftX, footerY + 28f, signerRolePaint)

    val rightX = width - innerInset - 42f
    val dateLabelPaint = paint(Color.parseColor("#8A7A46"), 8f, garamond, Paint.Align.RIGHT).apply { applyLetterSpacing(this, 0.1f) }
    canvas.drawText(data.issueDateLabel.uppercase(), rightX, footerY - 16f, dateLabelPaint)
    val datePaint = paint(gold, 11f, garamondItalic, Paint.Align.RIGHT)
    canvas.drawText(data.issueDateText, rightX, footerY, datePaint)
}

/** Dispatches on the persisted `template` string ("traditional"|"modern"|"simple"|"corporate"|
 *  "elegant"|"ribbon"|"geometric"|"dark"), defaulting to Traditional for any unrecognized value. */
fun drawCertificate(
    template: String,
    context: Context,
    canvas: Canvas,
    width: Float,
    height: Float,
    data: CertificateRenderData,
    logoBitmap: Bitmap?,
    signatureBitmap: Bitmap?
) {
    when (template) {
        "modern" -> drawCertificateModern(context, canvas, width, height, data, logoBitmap, signatureBitmap)
        "simple" -> drawCertificateSimple(context, canvas, width, height, data, logoBitmap, signatureBitmap)
        "corporate" -> drawCertificateCorporate(context, canvas, width, height, data, logoBitmap, signatureBitmap)
        "elegant" -> drawCertificateElegant(context, canvas, width, height, data, logoBitmap, signatureBitmap)
        "ribbon" -> drawCertificateRibbon(context, canvas, width, height, data, logoBitmap, signatureBitmap)
        "geometric" -> drawCertificateGeometric(context, canvas, width, height, data, logoBitmap, signatureBitmap)
        "dark" -> drawCertificateDark(context, canvas, width, height, data, logoBitmap, signatureBitmap)
        else -> drawCertificateTraditional(context, canvas, width, height, data, logoBitmap, signatureBitmap)
    }
}
