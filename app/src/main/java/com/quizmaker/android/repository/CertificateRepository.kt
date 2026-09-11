package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.remote.dto.CertificateDesignDto
import com.quizmaker.android.data.remote.dto.CertificateDesignUpsertDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import javax.inject.Inject
import javax.inject.Singleton

/** Shared with the web app's Certificate Designer — same bucket/path convention its uploads use,
 *  so a logo/signature uploaded from either platform is found by both. */
private const val CERTIFICATE_BUCKET = "images"

/**
 * Reads/writes the `certificate_designs` table — the exact same table the web app's Certificate
 * Designer uses, so a design edited on one platform shows up on the other. Follows
 * SettingsRepository's read-then-insert/update convention: (user_id) isn't guaranteed to be the
 * primary key here either, and this codebase never relies on Postgrest's onConflict upsert.
 */
@Singleton
class CertificateRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    // Explicit column list rather than "*" — this app's SupabaseClient Json doesn't ignore unknown
    // keys, and the table also carries created_at/updated_at that CertificateDesignDto doesn't
    // declare (same trap documented in SettingsRepository).
    private val designColumns = Columns.raw(
        "id,user_id,title,body_text,company_name,signer_name,signer_role,logo_url,signature_url,issue_date_label,template"
    )

    suspend fun getDesign(userId: String): AppResult<CertificateDesignDto?> = safeCall {
        supabase.from("certificate_designs")
            .select(designColumns) { filter { eq("user_id", userId) } }
            .decodeSingleOrNull<CertificateDesignDto>()
    }

    suspend fun upsertDesign(userId: String, design: CertificateDesignDto): AppResult<Unit> = safeCall {
        val existing = (getDesign(userId) as? AppResult.Success)?.data
        val payload = CertificateDesignUpsertDto(
            userId = userId,
            title = design.title,
            bodyText = design.bodyText,
            companyName = design.companyName,
            signerName = design.signerName,
            signerRole = design.signerRole,
            logoUrl = design.logoUrl,
            signatureUrl = design.signatureUrl,
            issueDateLabel = design.issueDateLabel,
            template = design.template
        )
        if (existing != null) {
            supabase.from("certificate_designs").update(payload) { filter { eq("user_id", userId) } }
        } else {
            supabase.from("certificate_designs").insert(payload)
        }
        Unit
    }

    suspend fun uploadLogo(userId: String, bytes: ByteArray, contentType: String, extension: String): AppResult<String> =
        upload("certificate-logo/$userId/${System.currentTimeMillis()}.$extension", bytes, contentType)

    suspend fun uploadSignature(userId: String, bytes: ByteArray, contentType: String, extension: String): AppResult<String> =
        upload("certificate-signature/$userId/${System.currentTimeMillis()}.$extension", bytes, contentType)

    private suspend fun upload(path: String, bytes: ByteArray, contentType: String): AppResult<String> = safeCall {
        val bucket = supabase.storage.from(CERTIFICATE_BUCKET)
        bucket.upload(path, bytes) {
            upsert = true
            this.contentType = ContentType.parse(contentType)
        }
        bucket.publicUrl(path)
    }
}
