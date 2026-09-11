package com.quizmaker.android.data.remote.dto

import com.quizmaker.android.data.model.OmrLayout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Row shape of the `omr_sheets` table (see supabase/migrations/20260910120000_add_omr_sheets.sql).
 * `layout` is a `jsonb` column — this app's SupabaseClient (see core/supabase/SupabaseModule.kt)
 * installs Postgrest with no custom Json{} block, so it serializes the whole row through
 * kotlinx.serialization's default Json the same way every other DTO here does; a plain nested
 * `@Serializable` data class (rather than a raw JsonElement/@Contextual wrapper) round-trips a
 * jsonb column exactly the same way postgrest-kt's own Json config expects, with no extra plumbing
 * needed — there's no other jsonb-column DTO in this codebase to mirror, so this is the
 * straightforward mapping rather than a confirmed-against-precedent one (call this out in review).
 */
@Serializable
data class OmrSheetDto(
    val id: String? = null,
    @SerialName("quiz_id") val quizId: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("page_width") val pageWidth: Float,
    @SerialName("page_height") val pageHeight: Float,
    val layout: OmrLayout,
    @SerialName("created_at") val createdAt: String? = null
)

/**
 * Insert payload for `omr_sheets` — deliberately omits `id`/`created_at` rather than reusing
 * [OmrSheetDto] with those left null, mirroring every other insert in this codebase
 * (QuizResponseInsertDto vs. QuizResponseDto, QuizInsertDto vs. QuizDto, CertificateDesignUpsertDto
 * vs. CertificateDesignDto): if a nullable `id`/`created_at` field were serialized as an explicit
 * JSON `null` in the insert body, Postgrest would send that literal null through, overriding the
 * column's Postgres default (`gen_random_uuid()` / `now()`) instead of leaving it unset — which
 * would fail outright for `id`, a NOT NULL primary key.
 */
@Serializable
data class OmrSheetInsertDto(
    @SerialName("quiz_id") val quizId: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("page_width") val pageWidth: Float,
    @SerialName("page_height") val pageHeight: Float,
    val layout: OmrLayout
)
