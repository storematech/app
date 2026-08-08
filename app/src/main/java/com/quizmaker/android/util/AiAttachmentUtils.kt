package com.quizmaker.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Reads/compresses PDFs and photos picked or captured for the AI quiz feature, and base64-encodes
 * them for the `generate-quiz-ai` edge function. Nothing here writes to Supabase — files only ever
 * live in the app's own cache dir (camera captures) or are read transiently (PDF picks), matching
 * the "don't store uploads, they cost money" constraint the AI feature was built under.
 */
object AiAttachmentUtils {
    const val MAX_PDF_BYTES = 8 * 1024 * 1024 // 8MB raw file
    const val MAX_IMAGES = 5
    private const val IMAGE_MAX_DIMENSION = 1600
    private const val IMAGE_JPEG_QUALITY = 80

    sealed class AttachmentResult {
        data class Success(val base64: String) : AttachmentResult()
        data class Error(val message: String) : AttachmentResult()
    }

    fun readPdfAsBase64(context: Context, uri: Uri): AttachmentResult {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return AttachmentResult.Error("Couldn't read that file.")
            if (bytes.isEmpty()) return AttachmentResult.Error("That PDF appears to be empty.")
            if (bytes.size > MAX_PDF_BYTES) {
                return AttachmentResult.Error("That PDF is too large (max 8MB). Try a smaller file.")
            }
            AttachmentResult.Success(Base64.encodeToString(bytes, Base64.NO_WRAP))
        } catch (e: Exception) {
            AttachmentResult.Error("Couldn't read that PDF.")
        }
    }

    /** Downscales + rotates (per EXIF) + JPEG-compresses so photos stay small before base64/upload. */
    fun compressImageAsBase64(context: Context, uri: Uri): AttachmentResult {
        return try {
            val bitmap = decodeSampledBitmap(context, uri) ?: return AttachmentResult.Error("Couldn't read that photo.")
            val rotated = applyExifRotation(context, uri, bitmap)
            val output = ByteArrayOutputStream()
            rotated.compress(Bitmap.CompressFormat.JPEG, IMAGE_JPEG_QUALITY, output)
            if (rotated !== bitmap) bitmap.recycle()
            rotated.recycle()
            AttachmentResult.Success(Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP))
        } catch (e: Exception) {
            AttachmentResult.Error("Couldn't process that photo.")
        }
    }

    fun queryFileName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) return cursor.getString(nameIndex)
        }
        return "document.pdf"
    }

    /** Creates a fresh cache-dir file (via FileProvider) for the camera app to write a capture into. */
    fun createCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, "ai_captures").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        var sampleSize = 1
        while (boundsOptions.outWidth / (sampleSize * 2) >= IMAGE_MAX_DIMENSION &&
            boundsOptions.outHeight / (sampleSize * 2) >= IMAGE_MAX_DIMENSION
        ) {
            sampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: return null

        if (decoded.width <= IMAGE_MAX_DIMENSION && decoded.height <= IMAGE_MAX_DIMENSION) return decoded
        val scale = IMAGE_MAX_DIMENSION.toFloat() / maxOf(decoded.width, decoded.height)
        val scaled = Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap

        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
