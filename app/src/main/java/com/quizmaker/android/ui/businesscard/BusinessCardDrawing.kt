package com.quizmaker.android.ui.businesscard

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

/** Standard business-card proportions (3.5in x 2in) at 72dpi — same point-based sizing convention
 *  as this app's other Canvas-drawn exports (see CertificateDrawing's CERTIFICATE_WIDTH/HEIGHT).
 *  Shared by the on-screen Compose live preview (scaled down) and BusinessCardImageExporter (scaled
 *  up to a print-quality bitmap) — both call [drawBusinessCard] with these same dimensions. */
const val BUSINESS_CARD_WIDTH = 252f
const val BUSINESS_CARD_HEIGHT = 144f

enum class BusinessCardTemplate { CLASSIC, MODERN, MINIMAL, BOLD, CORPORATE }

/** Every field but [personName]/[businessName] is optional — every template below skips a blank
 *  field entirely rather than rendering an empty line or a dangling separator. */
data class BusinessCardRenderData(
    val personName: String,
    val businessName: String,
    val tagline: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val website: String?
)

// ---- Shared drawing helpers (file-private copies, same duplication CertificateDrawing.kt's own
// helpers already accepted across this app's Canvas-drawn exporters — see e.g. OmrSheetPdfExporter
// copying MasterPaperPdfExporter's text helpers rather than trying to share private functions
// across files) -----------------------------------------------------------------------------------

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
    val words = text.split(" ")
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

private fun Canvas.drawWrapped(text: String, x: Float, top: Float, maxWidth: Float, paint: Paint, lineHeight: Float): Float {
    var y = top
    wrapText(text, paint, maxWidth).forEach { line ->
        drawText(line, x, y, paint)
        y += lineHeight
    }
    return y
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

/** Circular badge with a single initial letter — the logo stand-in used by every template below
 *  when no logo image is set. */
private fun Canvas.drawInitialBadge(centerX: Float, centerY: Float, radius: Float, badgeColor: Int, initial: String, textColor: Int, tf: Typeface?) {
    drawCircle(centerX, centerY, radius, paint(badgeColor, 0f))
    val textPaint = paint(textColor, radius * 1.05f, tf, Paint.Align.CENTER)
    val fm = textPaint.fontMetrics
    drawText(initial, centerX, centerY - (fm.ascent + fm.descent) / 2f, textPaint)
}

/** [phone]/[email]/[website] joined with a thin bullet separator, skipping any that are null/blank
 *  — never a dangling "•" with nothing on one side. Empty string if every field is blank. */
private fun joinContactLine(vararg parts: String?): String =
    parts.filter { !it.isNullOrBlank() }.joinToString("   •   ")

fun drawBusinessCard(
    template: BusinessCardTemplate,
    context: Context,
    canvas: Canvas,
    width: Float,
    height: Float,
    data: BusinessCardRenderData,
    logoBitmap: Bitmap?,
    accentColor: Int
) {
    when (template) {
        BusinessCardTemplate.CLASSIC -> drawClassic(context, canvas, width, height, data, logoBitmap)
        BusinessCardTemplate.MODERN -> drawModern(context, canvas, width, height, data, logoBitmap, accentColor)
        BusinessCardTemplate.MINIMAL -> drawMinimal(context, canvas, width, height, data, logoBitmap, accentColor)
        BusinessCardTemplate.BOLD -> drawBold(context, canvas, width, height, data, logoBitmap, accentColor)
        BusinessCardTemplate.CORPORATE -> drawCorporate(context, canvas, width, height, data, logoBitmap, accentColor)
    }
}

// ---- Classic — white, bronze double border, centered elegant serif ---------------------------

private fun drawClassic(context: Context, canvas: Canvas, width: Float, height: Float, data: BusinessCardRenderData, logoBitmap: Bitmap?) {
    val bronze = Color.parseColor("#B8860B")
    canvas.drawColor(Color.WHITE)

    canvas.drawRect(6f, 6f, width - 6f, height - 6f, paint(bronze, 0f).apply { style = Paint.Style.STROKE; strokeWidth = 1.4f })
    canvas.drawRect(10f, 10f, width - 10f, height - 10f, paint(bronze, 0f).apply { style = Paint.Style.STROKE; strokeWidth = 0.6f })

    val centerX = width / 2f
    val garamond = typefaceOf(context, R.font.eb_garamond)
    val garamondItalic = typefaceOf(context, R.font.eb_garamond_italic)
    var y = 32f

    if (logoBitmap != null) {
        canvas.drawBitmapCentered(logoBitmap, centerX, y, 26f)
        y += 20f
    }

    val businessPaint = paint(Color.parseColor("#1F1F1F"), 13f, garamond, Paint.Align.CENTER)
    canvas.drawText(data.businessName.uppercase(), centerX, y, businessPaint)
    y += 16f

    val personLine = listOfNotNull(data.personName.takeIf { it.isNotBlank() }, data.tagline?.takeIf { it.isNotBlank() }).joinToString("  —  ")
    if (personLine.isNotBlank()) {
        canvas.drawText(personLine, centerX, y, paint(Color.parseColor("#666666"), 8f, garamondItalic, Paint.Align.CENTER))
        y += 14f
    }

    y += 4f
    canvas.drawLine(centerX - 46f, y, centerX + 46f, y, paint(bronze, 0f).apply { strokeWidth = 0.6f })
    y += 14f

    val contactLine = joinContactLine(data.phone, data.email, data.website)
    if (contactLine.isNotBlank()) {
        canvas.drawText(contactLine, centerX, y, paint(Color.parseColor("#333333"), 7f, garamond, Paint.Align.CENTER))
        y += 12f
    }
    data.address?.takeIf { it.isNotBlank() }?.let { address ->
        val addressPaint = paint(Color.parseColor("#777777"), 6.5f, garamond, Paint.Align.CENTER)
        wrapText(address, addressPaint, width - 60f).forEach { line ->
            canvas.drawText(line, centerX, y, addressPaint)
            y += 10f
        }
    }
}

// ---- Modern — bold accent-color left band, Poppins throughout --------------------------------

private fun drawModern(context: Context, canvas: Canvas, width: Float, height: Float, data: BusinessCardRenderData, logoBitmap: Bitmap?, accentColor: Int) {
    canvas.drawColor(Color.WHITE)
    val bandWidth = 84f
    canvas.drawRect(0f, 0f, bandWidth, height, paint(accentColor, 0f))

    val bandCenterX = bandWidth / 2f
    val poppinsSemibold = typefaceOf(context, R.font.poppins_semibold)
    if (logoBitmap != null) {
        canvas.drawBitmapInCircle(logoBitmap, bandCenterX, height / 2f, 28f)
    } else {
        canvas.drawInitialBadge(
            bandCenterX, height / 2f, 26f,
            badgeColor = Color.WHITE,
            initial = data.businessName.trim().take(1).uppercase().ifBlank { "?" },
            textColor = accentColor,
            tf = poppinsSemibold
        )
    }

    val leftX = bandWidth + 14f
    val maxWidth = width - leftX - 14f
    val poppinsBold = typefaceOf(context, R.font.poppins_bold)
    val poppinsRegular = typefaceOf(context, R.font.poppins_regular)
    var y = 28f

    y = canvas.drawWrapped(data.businessName, leftX, y, maxWidth, paint(Color.parseColor("#111111"), 13f, poppinsBold), 15f)
    y += 2f

    val personLine = listOfNotNull(data.personName.takeIf { it.isNotBlank() }, data.tagline?.takeIf { it.isNotBlank() }).joinToString(" · ")
    if (personLine.isNotBlank()) {
        y = canvas.drawWrapped(personLine, leftX, y, maxWidth, paint(accentColor, 8.5f, poppinsSemibold), 12f)
        y += 4f
    }

    canvas.drawLine(leftX, y, width - 14f, y, paint(Color.parseColor("#E5E7EB"), 0f).apply { strokeWidth = 0.6f })
    y += 12f

    val contactPaint = paint(Color.parseColor("#444444"), 7.5f, poppinsRegular)
    listOfNotNull(data.phone, data.email, data.website).filter { it.isNotBlank() }.forEach { line ->
        canvas.drawText(line, leftX, y, contactPaint)
        y += 11f
    }
    data.address?.takeIf { it.isNotBlank() }?.let { address ->
        y = canvas.drawWrapped(address, leftX, y, maxWidth, contactPaint, 11f)
    }
}

// ---- Minimal — white space, one accent rule, left-aligned monochrome type --------------------

private fun drawMinimal(context: Context, canvas: Canvas, width: Float, height: Float, data: BusinessCardRenderData, logoBitmap: Bitmap?, accentColor: Int) {
    canvas.drawColor(Color.WHITE)
    val leftX = 20f
    val maxWidth = width - 40f
    val poppinsSemibold = typefaceOf(context, R.font.poppins_semibold)
    val poppinsRegular = typefaceOf(context, R.font.poppins_regular)
    var y = 30f

    if (logoBitmap != null) {
        canvas.drawBitmapCentered(logoBitmap, leftX + 12f, 22f, 22f)
        y += 4f
    }

    val businessPaint = paint(Color.parseColor("#111111"), 11f, poppinsSemibold).apply { letterSpacing = 0.1f }
    canvas.drawText(data.businessName.uppercase(), leftX, y, businessPaint)
    y += 14f

    val personLine = listOfNotNull(data.personName.takeIf { it.isNotBlank() }, data.tagline?.takeIf { it.isNotBlank() }).joinToString(" · ")
    if (personLine.isNotBlank()) {
        canvas.drawText(personLine, leftX, y, paint(Color.parseColor("#777777"), 8f, poppinsRegular))
        y += 14f
    }

    y += 4f
    canvas.drawLine(leftX, y, leftX + 40f, y, paint(accentColor, 0f).apply { strokeWidth = 2f })
    y += 16f

    val contactPaint = paint(Color.parseColor("#333333"), 7.5f, poppinsRegular)
    listOfNotNull(data.phone, data.email, data.website).filter { it.isNotBlank() }.forEach { line ->
        canvas.drawText(line, leftX, y, contactPaint)
        y += 11f
    }
    data.address?.takeIf { it.isNotBlank() }?.let { address ->
        canvas.drawWrapped(address, leftX, y, maxWidth, contactPaint, 11f)
    }
}

// ---- Bold — full accent-color background, white type -----------------------------------------

private fun drawBold(context: Context, canvas: Canvas, width: Float, height: Float, data: BusinessCardRenderData, logoBitmap: Bitmap?, accentColor: Int) {
    canvas.drawColor(accentColor)

    val leftX = 18f
    val maxWidth = width - 36f
    val poppinsBold = typefaceOf(context, R.font.poppins_bold)
    val poppinsMedium = typefaceOf(context, R.font.poppins_medium)
    val poppinsRegular = typefaceOf(context, R.font.poppins_regular)

    if (logoBitmap != null) {
        canvas.drawBitmapInCircle(logoBitmap, width - 32f, 30f, 22f)
    } else {
        canvas.drawInitialBadge(
            width - 32f, 30f, 20f,
            badgeColor = Color.WHITE,
            initial = data.businessName.trim().take(1).uppercase().ifBlank { "?" },
            textColor = accentColor,
            tf = poppinsBold
        )
    }

    var y = 46f
    y = canvas.drawWrapped(data.businessName, leftX, y, maxWidth - 50f, paint(Color.WHITE, 16f, poppinsBold), 18f)
    y += 4f

    val personLine = listOfNotNull(data.personName.takeIf { it.isNotBlank() }, data.tagline?.takeIf { it.isNotBlank() }).joinToString(" · ")
    if (personLine.isNotBlank()) {
        y = canvas.drawWrapped(personLine, leftX, y, maxWidth, paint(Color.argb(230, 255, 255, 255), 9f, poppinsMedium), 12f)
    }

    val contactPaint = paint(Color.argb(210, 255, 255, 255), 7.5f, poppinsRegular)
    var footerY = height - 30f
    val footerLines = listOfNotNull(data.phone, data.email, data.website).filter { it.isNotBlank() }
    if (footerLines.isNotEmpty()) {
        canvas.drawText(footerLines.joinToString("   •   "), leftX, footerY, contactPaint)
        footerY += 11f
    }
    data.address?.takeIf { it.isNotBlank() }?.let { address ->
        canvas.drawWrapped(address, leftX, footerY, maxWidth, contactPaint, 10f)
    }
}

// ---- Corporate — dark navy header band, white body, two-column contact footer ------------------

private fun drawCorporate(context: Context, canvas: Canvas, width: Float, height: Float, data: BusinessCardRenderData, logoBitmap: Bitmap?, accentColor: Int) {
    val navy = Color.parseColor("#1E293B")
    canvas.drawColor(Color.WHITE)

    val bandHeight = 58f
    canvas.drawRect(0f, 0f, width, bandHeight, paint(navy, 0f))

    val poppinsBold = typefaceOf(context, R.font.poppins_bold)
    val poppinsMedium = typefaceOf(context, R.font.poppins_medium)
    val poppinsRegular = typefaceOf(context, R.font.poppins_regular)
    val leftX = 16f

    if (logoBitmap != null) {
        canvas.drawBitmapInCircle(logoBitmap, width - 30f, bandHeight / 2f, 18f)
    }

    var y = 24f
    y = canvas.drawWrapped(data.businessName, leftX, y, width - leftX - 44f, paint(Color.WHITE, 13f, poppinsBold), 15f)
    if (!data.tagline.isNullOrBlank()) {
        canvas.drawText(data.tagline, leftX, y, paint(Color.argb(200, 255, 255, 255), 8f, poppinsMedium))
    }

    if (data.personName.isNotBlank()) {
        canvas.drawText(data.personName, leftX, bandHeight + 16f, paint(navy, 9.5f, poppinsBold))
    }

    val colTop = bandHeight + (if (data.personName.isNotBlank()) 28f else 16f)
    val colDivider = width / 2f
    val addressPaint = paint(Color.parseColor("#334155"), 7f, poppinsRegular)
    data.address?.takeIf { it.isNotBlank() }?.let { address ->
        canvas.drawWrapped(address, leftX, colTop, colDivider - leftX - 8f, addressPaint, 10.5f)
    }

    val contactPaint = paint(Color.parseColor("#334155"), 7f, poppinsRegular)
    var contactY = colTop
    listOfNotNull(data.phone, data.email, data.website).filter { it.isNotBlank() }.forEach { line ->
        canvas.drawText(line, colDivider + 8f, contactY, contactPaint)
        contactY += 10.5f
    }

    canvas.drawLine(6f, height - 8f, 26f, height - 8f, paint(accentColor, 0f).apply { strokeWidth = 2f })
}
