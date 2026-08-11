package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Row shape of the `feedback_forms` table. Reuses [ToolFieldDto] (declared in OnboardingFormDto.kt, shared across all "Tools") for `questions`. */
@Serializable
data class FeedbackFormDto(
    val id: String,
    @SerialName("created_by") val createdBy: String? = null,
    val title: String,
    val description: String? = null,
    val slug: String,
    val questions: List<ToolFieldDto> = emptyList(),
    @SerialName("collect_identity") val collectIdentity: Boolean = true,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class FeedbackFormInsertDto(
    @SerialName("created_by") val createdBy: String,
    val title: String,
    val description: String?,
    val slug: String,
    val questions: List<ToolFieldDto>,
    @SerialName("collect_identity") val collectIdentity: Boolean,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class FeedbackFormUpdateDto(
    val title: String,
    val description: String?,
    val questions: List<ToolFieldDto>,
    @SerialName("collect_identity") val collectIdentity: Boolean,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class FeedbackFormActiveUpdateDto(
    @SerialName("is_active") val isActive: Boolean
)

/** Row shape of the `feedback_submissions` table — decode-only, mobile never writes submissions. */
@Serializable
data class FeedbackSubmissionDto(
    val id: String,
    @SerialName("form_id") val formId: String,
    @SerialName("learner_name") val learnerName: String? = null,
    @SerialName("learner_email") val learnerEmail: String? = null,
    @SerialName("overall_rating") val overallRating: Int? = null,
    val answers: JsonObject = JsonObject(emptyMap()),
    @SerialName("created_at") val createdAt: String
)
