package com.quizmaker.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The account's optional PDF letterhead (logo/business name/address — all null is the common
 *  case) plus its saved report design (accent color + template, defaulting to brand blue/Modern
 *  for every account that hasn't picked one yet). Every [com.quizmaker.android.util] *PdfExporter
 *  reads both off this one object. */
data class PdfBranding(
    val logo: Bitmap?,
    val businessName: String?,
    val address: String?,
    val accentColor: Int = ReportDesign.DEFAULT_ACCENT_COLOR,
    val template: ReportTemplate = ReportTemplate.MODERN
) {
    companion object {
        val NONE = PdfBranding(logo = null, businessName = null, address = null)
    }
}

/**
 * Loads the current account's `profiles.business_logo`/`address` and decodes the logo into a
 * software [Bitmap] (PdfDocument's Canvas can't draw a hardware bitmap) for the PDF exporters,
 * plus its saved [ReportDesign] (SettingsRepository). The bitmap is cached in memory per URL so
 * repeat exports in the same app session — a very likely pattern, e.g. exporting a leaderboard
 * right after a quiz detail view — don't re-download the same logo each time.
 */
@Singleton
class PdfBrandingProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) {
    private var cachedLogoUrl: String? = null
    private var cachedBitmap: Bitmap? = null

    /** Never throws — a failed profile/design fetch or logo download just falls back to the
     *  default look, not a blocked export. */
    suspend fun get(): PdfBranding {
        val profile = (authRepository.getCurrentProfile(notifyOnError = false) as? AppResult.Success)?.data
            ?: return PdfBranding.NONE
        val logoUrl = profile.businessLogo?.takeIf { it.isNotBlank() }
        val businessName = profile.businessName.takeIf { it.isNotBlank() }
        val address = profile.address?.takeIf { it.isNotBlank() }
        val design = (settingsRepository.getReportDesign(profile.id) as? AppResult.Success)?.data ?: ReportDesign.DEFAULT
        return PdfBranding(
            logo = logoUrl?.let { loadBitmap(it) },
            businessName = businessName,
            address = address,
            accentColor = design.accentColorInt(),
            template = design.template
        )
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
