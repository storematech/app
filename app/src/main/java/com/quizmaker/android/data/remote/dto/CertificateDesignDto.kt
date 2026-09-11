package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Row shape of the `certificate_designs` table — shared with the web app's Certificate Designer
 * (src/pages/CertificateDesigner or similar in the web repo). A design edited on one platform
 * must show up on the other, so this mirrors the web DTO's snake_case columns exactly. Only the
 * columns this app actually reads/writes are declared (not created_at/updated_at) — this app's
 * SupabaseClient Json doesn't ignore unknown keys, so CertificateRepository selects an explicit
 * column list rather than "*" to avoid a decode failure, same pattern as SettingsRepository.
 */
@Serializable
data class CertificateDesignDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    val title: String,
    @SerialName("body_text") val bodyText: String,
    @SerialName("company_name") val companyName: String,
    @SerialName("signer_name") val signerName: String,
    @SerialName("signer_role") val signerRole: String,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("signature_url") val signatureUrl: String? = null,
    @SerialName("issue_date_label") val issueDateLabel: String = "Date of Issue",
    val template: String = "traditional"
)

/** Insert/update payload — omits [CertificateDesignDto.id] (server-generated, never sent back). */
@Serializable
data class CertificateDesignUpsertDto(
    @SerialName("user_id") val userId: String,
    val title: String,
    @SerialName("body_text") val bodyText: String,
    @SerialName("company_name") val companyName: String,
    @SerialName("signer_name") val signerName: String,
    @SerialName("signer_role") val signerRole: String,
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("signature_url") val signatureUrl: String? = null,
    @SerialName("issue_date_label") val issueDateLabel: String = "Date of Issue",
    val template: String = "traditional"
)
