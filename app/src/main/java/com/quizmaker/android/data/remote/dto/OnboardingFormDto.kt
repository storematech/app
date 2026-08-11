package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** One entry of the `fields` jsonb array on `onboarding_forms` (and, later, `feedback_forms`/`surveys`). */
@Serializable
data class ToolFieldDto(
    val id: String,
    val label: String,
    val type: String,
    val required: Boolean,
    val options: List<String>? = null,
    val placeholder: String? = null
)

/** Row shape of the `onboarding_forms` table. */
@Serializable
data class OnboardingFormDto(
    val id: String,
    @SerialName("created_by") val createdBy: String? = null,
    val title: String,
    val description: String? = null,
    val slug: String,
    @SerialName("welcome_message") val welcomeMessage: String? = null,
    val fields: List<ToolFieldDto> = emptyList(),
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class OnboardingFormInsertDto(
    @SerialName("created_by") val createdBy: String,
    val title: String,
    val description: String?,
    val slug: String,
    @SerialName("welcome_message") val welcomeMessage: String?,
    val fields: List<ToolFieldDto>,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class OnboardingFormUpdateDto(
    val title: String,
    val description: String?,
    @SerialName("welcome_message") val welcomeMessage: String?,
    val fields: List<ToolFieldDto>,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class OnboardingFormActiveUpdateDto(
    @SerialName("is_active") val isActive: Boolean
)

/** Row shape of the `onboarding_submissions` table — decode-only, mobile never writes submissions. */
@Serializable
data class OnboardingSubmissionDto(
    val id: String,
    @SerialName("form_id") val formId: String,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val answers: JsonObject = JsonObject(emptyMap()),
    @SerialName("created_at") val createdAt: String
)
