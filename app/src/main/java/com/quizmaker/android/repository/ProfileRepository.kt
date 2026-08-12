package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.remote.dto.ProfileFcmTokenUpdateDto
import com.quizmaker.android.data.remote.dto.ProfileLogoUpdateDto
import com.quizmaker.android.data.remote.dto.ProfilePhoneUpdateDto
import com.quizmaker.android.data.remote.dto.ProfileUpdateDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import javax.inject.Inject
import javax.inject.Singleton

/** The account's PDF letterhead logo lives here — public bucket, one object per user (fixed path, upsert on re-upload). */
private const val LOGO_BUCKET = "business-logos"

/** Updates the `profiles` table — mirrors the Account tab in the web app's Settings.tsx. */
@Singleton
class ProfileRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun updateProfile(
        userId: String,
        name: String,
        businessName: String,
        phoneNumber: String,
        country: String,
        address: String
    ): AppResult<Unit> = safeCall {
        supabase.from("profiles")
            .update(
                ProfileUpdateDto(
                    name = name,
                    businessName = businessName,
                    phoneNumber = phoneNumber,
                    country = country,
                    address = address
                )
            ) { filter { eq("id", userId) } }
        Unit
    }

    /**
     * Uploads the PDF letterhead logo to a fixed per-user path (upsert — re-uploading always
     * overwrites the same object rather than accumulating orphaned files) and points
     * `profiles.business_logo` at its public URL. [contentType] is passed explicitly (e.g.
     * "image/jpeg") since the path itself carries no file extension to infer it from.
     */
    suspend fun uploadLogo(userId: String, bytes: ByteArray, contentType: String): AppResult<String> = safeCall {
        val path = "$userId/logo"
        val bucket = supabase.storage.from(LOGO_BUCKET)
        bucket.upload(path, bytes) {
            upsert = true
            this.contentType = ContentType.parse(contentType)
        }
        val url = bucket.publicUrl(path)
        supabase.from("profiles").update(ProfileLogoUpdateDto(businessLogo = url)) { filter { eq("id", userId) } }
        url
    }

    suspend fun removeLogo(userId: String): AppResult<Unit> = safeCall {
        // Best-effort — an already-missing storage object shouldn't block clearing the DB column.
        runCatching { supabase.storage.from(LOGO_BUCKET).delete("$userId/logo") }
        supabase.from("profiles").update(ProfileLogoUpdateDto(businessLogo = null)) { filter { eq("id", userId) } }
        Unit
    }

    /** Onboarding's one-time phone collection — only touches phone_number/country, never name/business_name. */
    suspend fun updatePhoneNumber(userId: String, phoneNumber: String, country: String): AppResult<Unit> = safeCall {
        supabase.from("profiles")
            .update(ProfilePhoneUpdateDto(phoneNumber = phoneNumber, country = country)) {
                filter { eq("id", userId) }
            }
        Unit
    }

    /** Called from QuizFcmService.onNewToken() and once per Dashboard load — only touches fcm_token. */
    suspend fun updateFcmToken(userId: String, token: String): AppResult<Unit> = safeCall {
        supabase.from("profiles")
            .update(ProfileFcmTokenUpdateDto(fcmToken = token)) {
                filter { eq("id", userId) }
            }
        Unit
    }
}
