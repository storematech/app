package com.quizmaker.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The account's optional PDF letterhead — a logo bitmap and/or an address line. Both null is the common case, and every [com.quizmaker.android.util] *PdfExporter draws nothing extra for it. */
data class PdfBranding(val logo: Bitmap?, val address: String?) {
    companion object {
        val NONE = PdfBranding(logo = null, address = null)
    }
}

/**
 * Loads the current account's `profiles.business_logo`/`address` and decodes the logo into a
 * software [Bitmap] (PdfDocument's Canvas can't draw a hardware bitmap) for the PDF exporters.
 * The bitmap is cached in memory per URL so repeat exports in the same app session — a very
 * likely pattern, e.g. exporting a leaderboard right after a quiz detail view — don't re-download
 * the same logo each time.
 */
@Singleton
class PdfBrandingProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) {
    private var cachedLogoUrl: String? = null
    private var cachedBitmap: Bitmap? = null

    /** Never throws — a failed profile fetch or logo download just means no letterhead, not a blocked export. */
    suspend fun get(): PdfBranding {
        val profile = (authRepository.getCurrentProfile(notifyOnError = false) as? AppResult.Success)?.data
            ?: return PdfBranding.NONE
        val logoUrl = profile.businessLogo?.takeIf { it.isNotBlank() }
        val address = profile.address?.takeIf { it.isNotBlank() }
        return PdfBranding(logo = logoUrl?.let { loadBitmap(it) }, address = address)
    }

    private suspend fun loadBitmap(url: String): Bitmap? {
        cachedBitmap?.takeIf { url == cachedLogoUrl }?.let { return it }
        return runCatching {
            val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
            val drawable = context.imageLoader.execute(request).drawable as? BitmapDrawable
            drawable?.bitmap?.also {
                cachedLogoUrl = url
                cachedBitmap = it
            }
        }.getOrNull()
    }
}
