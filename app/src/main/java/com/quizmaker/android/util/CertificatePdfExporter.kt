package com.quizmaker.android.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import coil.imageLoader
import coil.request.ImageRequest
import com.quizmaker.android.ui.certificate.CERTIFICATE_HEIGHT
import com.quizmaker.android.ui.certificate.CERTIFICATE_WIDTH
import com.quizmaker.android.ui.certificate.CertificateRenderData
import com.quizmaker.android.ui.certificate.drawCertificate
import java.io.File
import java.io.FileOutputStream

/** Renders a single-page certificate PDF (landscape, same CERTIFICATE_WIDTH/HEIGHT the on-screen
 *  preview draws at) and returns a shareable file/Intent — same single-page PdfDocument convention
 *  as QrFlyerPdfExporter. */
object CertificatePdfExporter {

    suspend fun export(
        context: Context,
        template: String,
        data: CertificateRenderData,
        logoUrl: String?,
        signatureUrl: String?
    ): Intent {
        val file = renderFile(context, template, data, logoUrl, signatureUrl)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun renderFile(
        context: Context,
        template: String,
        data: CertificateRenderData,
        logoUrl: String?,
        signatureUrl: String?
    ): File {
        val logoBitmap = logoUrl?.let { loadBitmap(context, it) }
        val signatureBitmap = signatureUrl?.let { loadBitmap(context, it) }

        val document = PdfDocument()
        val page = document.startPage(
            PdfDocument.PageInfo.Builder(CERTIFICATE_WIDTH.toInt(), CERTIFICATE_HEIGHT.toInt(), 1).create()
        )
        drawCertificate(template, context, page.canvas, CERTIFICATE_WIDTH, CERTIFICATE_HEIGHT, data, logoBitmap, signatureBitmap)
        document.finishPage(page)

        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = data.recipientName.ifBlank { "certificate" }.replace(Regex("[^A-Za-z0-9]+"), "_")
        val file = File(exportsDir, "${safeName}_certificate.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        return file
    }

    /** Never throws — a failed image download just draws without that image, same fallback philosophy as PdfBrandingProvider. */
    private suspend fun loadBitmap(context: Context, url: String): Bitmap? = runCatching {
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
        (context.imageLoader.execute(request).drawable as? BitmapDrawable)?.bitmap
    }.getOrNull()
}
