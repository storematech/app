package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.quizmaker.android.ui.businesscard.BUSINESS_CARD_HEIGHT
import com.quizmaker.android.ui.businesscard.BUSINESS_CARD_WIDTH
import com.quizmaker.android.ui.businesscard.BusinessCardRenderData
import com.quizmaker.android.ui.businesscard.BusinessCardTemplate
import com.quizmaker.android.ui.businesscard.drawBusinessCard
import java.io.File
import java.io.FileOutputStream

/**
 * Renders [drawBusinessCard] onto a print-quality PNG (a business card is a digital-sharing
 * artifact — WhatsApp/email/save-to-contacts — not a print-a-full-page document like the
 * Certificate/Master Paper PDFs, hence an image export rather than a PdfDocument one) and returns
 * a shareable Intent — same FileProvider/cacheDir convention as CertificatePdfExporter/
 * QrFlyerPdfExporter, just a PNG instead of a PDF.
 */
object BusinessCardImageExporter {

    /** 300dpi over the 252x144pt card = 1050x600px — the same "draw at the small point-space, scale
     *  the canvas up" trick the on-screen preview uses at a much smaller scale for its live render. */
    private const val EXPORT_SCALE = 300f / 72f

    fun export(
        context: Context,
        template: BusinessCardTemplate,
        data: BusinessCardRenderData,
        logoBitmap: Bitmap?,
        accentColor: Int
    ): Intent {
        val widthPx = (BUSINESS_CARD_WIDTH * EXPORT_SCALE).toInt()
        val heightPx = (BUSINESS_CARD_HEIGHT * EXPORT_SCALE).toInt()
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.scale(EXPORT_SCALE, EXPORT_SCALE)
        drawBusinessCard(template, context, canvas, BUSINESS_CARD_WIDTH, BUSINESS_CARD_HEIGHT, data, logoBitmap, accentColor)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = data.businessName.ifBlank { data.personName }.ifBlank { "business_card" }.replace(Regex("[^A-Za-z0-9]+"), "_")
        val file = File(exportsDir, "${safeName}_business_card.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
