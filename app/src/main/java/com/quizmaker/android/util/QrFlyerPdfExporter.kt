package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/** Renders a printable one-page "scan to take the quiz" flyer (branded header, QR, instructions, link) and returns a share Intent for it. */
object QrFlyerPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val HEADER_HEIGHT = 150f

    fun export(context: Context, quizTitle: String, shareUrl: String, qrBitmap: Bitmap): Intent {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val canvas = page.canvas

        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6366F1") }
        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 26f; isFakeBoldText = true; textAlign = Paint.Align.CENTER
        }
        val headerSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0E7FF"); textSize = 13f; textAlign = Paint.Align.CENTER
        }
        val quizNameBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F8FAFC") }
        val quizNameBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5E7EB"); style = Paint.Style.STROKE; strokeWidth = 1f
        }
        val quizNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827"); textSize = 16f; isFakeBoldText = true; textAlign = Paint.Align.CENTER
        }
        val qrBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E5E7EB"); style = Paint.Style.STROKE; strokeWidth = 1f
        }
        val stepsHeadingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827"); textSize = 15f; isFakeBoldText = true; textAlign = Paint.Align.CENTER
        }
        val stepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563"); textSize = 12f; textAlign = Paint.Align.CENTER
        }
        val linkLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827"); textSize = 12f; isFakeBoldText = true; textAlign = Paint.Align.CENTER
        }
        val linkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6366F1"); textSize = 11f; textAlign = Paint.Align.CENTER
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9CA3AF"); textSize = 10f; textAlign = Paint.Align.CENTER
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 1f }

        val centerX = PAGE_WIDTH / 2f

        // Header band
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), HEADER_HEIGHT, headerBgPaint)
        canvas.drawText("Scan to Take the Quiz", centerX, 70f, headerTitlePaint)
        canvas.drawText("Quick • Easy • Mobile-friendly", centerX, 95f, headerSubtitlePaint)

        var y = HEADER_HEIGHT + 46f

        // Quiz name chip
        val chipHeight = 46f
        val chipRect = android.graphics.RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + chipHeight)
        canvas.drawRoundRect(chipRect, 12f, 12f, quizNameBgPaint)
        canvas.drawRoundRect(chipRect, 12f, 12f, quizNameBorderPaint)
        canvas.drawText(quizTitle.ifBlank { "Untitled quiz" }, centerX, y + chipHeight / 2f + 5f, quizNamePaint)
        y += chipHeight + 36f

        // QR code, centered
        val qrSize = 260f
        val qrLeft = centerX - qrSize / 2f
        val qrPadding = 14f
        val qrBoxRect = android.graphics.RectF(qrLeft - qrPadding, y - qrPadding, qrLeft + qrSize + qrPadding, y + qrSize + qrPadding)
        canvas.drawRoundRect(qrBoxRect, 16f, 16f, qrBorderPaint)
        val qrDestRect = android.graphics.RectF(qrLeft, y, qrLeft + qrSize, y + qrSize)
        canvas.drawBitmap(qrBitmap, null, qrDestRect, null)
        y += qrSize + qrPadding + 40f

        // How to take the quiz
        canvas.drawText("How to take the quiz", centerX, y, stepsHeadingPaint)
        y += 22f
        listOf(
            "1. Open the camera app on your phone",
            "2. Point it at the QR code above",
            "3. Tap the link to start the quiz"
        ).forEach { step ->
            canvas.drawText(step, centerX, y, stepPaint)
            y += 18f
        }
        y += 14f

        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
        y += 26f

        canvas.drawText("Or visit:", centerX, y, linkLabelPaint)
        y += 18f
        canvas.drawText(shareUrl, centerX, y, linkPaint)
        y += 30f

        canvas.drawText("Powered by Yuno LMS", centerX, y, footerPaint)

        document.finishPage(page)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = quizTitle.ifBlank { "quiz" }.replace(Regex("[^A-Za-z0-9]+"), "_")
        val file = File(exportsDir, "${safeName}_qr_flyer.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
