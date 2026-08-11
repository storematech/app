package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.FeedbackForm
import com.quizmaker.android.data.model.FeedbackSubmission
import com.quizmaker.android.data.model.ToolField
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.model.toDto
import com.quizmaker.android.data.remote.dto.FeedbackFormActiveUpdateDto
import com.quizmaker.android.data.remote.dto.FeedbackFormDto
import com.quizmaker.android.data.remote.dto.FeedbackFormInsertDto
import com.quizmaker.android.data.remote.dto.FeedbackFormUpdateDto
import com.quizmaker.android.data.remote.dto.FeedbackSubmissionDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Minimal row shape for the narrow `form_id`-only select in getSubmissionCounts(). */
@Serializable
private data class FeedbackFormIdRow(@SerialName("form_id") val formId: String)

/**
 * Reads/writes `feedback_forms`/`feedback_submissions` — the second of the "Tools" (More → Tools),
 * teacher-facing form management only. Filling out a form via its share link stays a web-only flow.
 * Same shape as OnboardingFormRepository.
 */
@Singleton
class FeedbackFormRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getForms(userId: String): AppResult<List<FeedbackForm>> = safeCall {
        supabase.from("feedback_forms")
            .select {
                filter { eq("created_by", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<FeedbackFormDto>()
            .map { it.toDomain() }
    }

    suspend fun getForm(formId: String): AppResult<FeedbackForm> = safeCall {
        supabase.from("feedback_forms")
            .select { filter { eq("id", formId) } }
            .decodeSingle<FeedbackFormDto>()
            .toDomain()
    }

    suspend fun getSubmissionCounts(formIds: List<String>): AppResult<Map<String, Int>> = safeCall {
        if (formIds.isEmpty()) return@safeCall emptyMap()
        supabase.from("feedback_submissions")
            .select(Columns.raw("form_id")) { filter { isIn("form_id", formIds) } }
            .decodeList<FeedbackFormIdRow>()
            .groupingBy { it.formId }
            .eachCount()
    }

    suspend fun createForm(
        userId: String,
        title: String,
        description: String?,
        questions: List<ToolField>,
        collectIdentity: Boolean,
        isActive: Boolean
    ): AppResult<FeedbackForm> = safeCall {
        supabase.from("feedback_forms")
            .insert(
                FeedbackFormInsertDto(
                    createdBy = userId,
                    title = title,
                    description = description,
                    slug = generateSlug(title),
                    questions = questions.map { it.toDto() },
                    collectIdentity = collectIdentity,
                    isActive = isActive
                )
            ) { select() }
            .decodeSingle<FeedbackFormDto>()
            .toDomain()
    }

    suspend fun updateForm(
        formId: String,
        title: String,
        description: String?,
        questions: List<ToolField>,
        collectIdentity: Boolean,
        isActive: Boolean
    ): AppResult<FeedbackForm> = safeCall {
        supabase.from("feedback_forms")
            .update(
                FeedbackFormUpdateDto(
                    title = title,
                    description = description,
                    questions = questions.map { it.toDto() },
                    collectIdentity = collectIdentity,
                    isActive = isActive
                )
            ) {
                filter { eq("id", formId) }
                select()
            }
            .decodeSingle<FeedbackFormDto>()
            .toDomain()
    }

    suspend fun setActive(formId: String, isActive: Boolean): AppResult<Unit> = safeCall {
        supabase.from("feedback_forms").update(FeedbackFormActiveUpdateDto(isActive = isActive)) {
            filter { eq("id", formId) }
        }
        Unit
    }

    suspend fun deleteForm(formId: String): AppResult<Unit> = safeCall {
        supabase.from("feedback_forms").delete { filter { eq("id", formId) } }
        Unit
    }

    suspend fun getSubmissions(formId: String): AppResult<List<FeedbackSubmission>> = safeCall {
        supabase.from("feedback_submissions")
            .select {
                filter { eq("form_id", formId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<FeedbackSubmissionDto>()
            .map { it.toDomain() }
    }

    /** `slug` is unique-constrained — a short random suffix avoids a pre-check round trip for the common case. */
    private fun generateSlug(title: String): String {
        val base = title.lowercase().trim()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "form" }
        return "$base-${UUID.randomUUID().toString().take(6)}"
    }
}
