package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.OnboardingForm
import com.quizmaker.android.data.model.OnboardingSubmission
import com.quizmaker.android.data.model.ToolField
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.model.toDto
import com.quizmaker.android.data.remote.dto.OnboardingFormActiveUpdateDto
import com.quizmaker.android.data.remote.dto.OnboardingFormDto
import com.quizmaker.android.data.remote.dto.OnboardingFormInsertDto
import com.quizmaker.android.data.remote.dto.OnboardingFormUpdateDto
import com.quizmaker.android.data.remote.dto.OnboardingSubmissionDto
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
private data class FormIdRow(@SerialName("form_id") val formId: String)

/**
 * Reads/writes `onboarding_forms`/`onboarding_submissions` — the first of the "Tools" (More → Tools),
 * teacher-facing form management only. Filling out a form via its share link stays a web-only flow.
 */
@Singleton
class OnboardingFormRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getForms(userId: String): AppResult<List<OnboardingForm>> = safeCall {
        supabase.from("onboarding_forms")
            .select {
                filter { eq("created_by", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<OnboardingFormDto>()
            .map { it.toDomain() }
    }

    suspend fun getForm(formId: String): AppResult<OnboardingForm> = safeCall {
        supabase.from("onboarding_forms")
            .select { filter { eq("id", formId) } }
            .decodeSingle<OnboardingFormDto>()
            .toDomain()
    }

    suspend fun getSubmissionCounts(formIds: List<String>): AppResult<Map<String, Int>> = safeCall {
        if (formIds.isEmpty()) return@safeCall emptyMap()
        supabase.from("onboarding_submissions")
            .select(Columns.raw("form_id")) { filter { isIn("form_id", formIds) } }
            .decodeList<FormIdRow>()
            .groupingBy { it.formId }
            .eachCount()
    }

    suspend fun createForm(
        userId: String,
        title: String,
        description: String?,
        welcomeMessage: String?,
        fields: List<ToolField>,
        isActive: Boolean
    ): AppResult<OnboardingForm> = safeCall {
        supabase.from("onboarding_forms")
            .insert(
                OnboardingFormInsertDto(
                    createdBy = userId,
                    title = title,
                    description = description,
                    slug = generateSlug(title),
                    welcomeMessage = welcomeMessage,
                    fields = fields.map { it.toDto() },
                    isActive = isActive
                )
            ) { select() }
            .decodeSingle<OnboardingFormDto>()
            .toDomain()
    }

    suspend fun updateForm(
        formId: String,
        title: String,
        description: String?,
        welcomeMessage: String?,
        fields: List<ToolField>,
        isActive: Boolean
    ): AppResult<OnboardingForm> = safeCall {
        supabase.from("onboarding_forms")
            .update(
                OnboardingFormUpdateDto(
                    title = title,
                    description = description,
                    welcomeMessage = welcomeMessage,
                    fields = fields.map { it.toDto() },
                    isActive = isActive
                )
            ) {
                filter { eq("id", formId) }
                select()
            }
            .decodeSingle<OnboardingFormDto>()
            .toDomain()
    }

    suspend fun setActive(formId: String, isActive: Boolean): AppResult<Unit> = safeCall {
        supabase.from("onboarding_forms").update(OnboardingFormActiveUpdateDto(isActive = isActive)) {
            filter { eq("id", formId) }
        }
        Unit
    }

    suspend fun deleteForm(formId: String): AppResult<Unit> = safeCall {
        supabase.from("onboarding_forms").delete { filter { eq("id", formId) } }
        Unit
    }

    suspend fun getSubmissions(formId: String): AppResult<List<OnboardingSubmission>> = safeCall {
        supabase.from("onboarding_submissions")
            .select {
                filter { eq("form_id", formId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<OnboardingSubmissionDto>()
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
