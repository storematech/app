package com.quizmaker.android.data.model

import com.quizmaker.android.BuildConfig
import com.quizmaker.android.data.remote.dto.OnboardingFormDto
import com.quizmaker.android.data.remote.dto.OnboardingSubmissionDto
import com.quizmaker.android.data.remote.dto.ToolFieldDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/** Shared across all six "Tools" (Onboarding, Feedback, Poll, Voting, RSVP, Survey) — same field-type contract as the web app's types/tools.ts. */
enum class ToolFieldType(val value: String, val label: String) {
    SHORT_TEXT("short_text", "Short text"),
    LONG_TEXT("long_text", "Paragraph"),
    EMAIL("email", "Email"),
    PHONE("phone", "Phone"),
    NUMBER("number", "Number"),
    DATE("date", "Date"),
    SELECT("select", "Dropdown"),
    RADIO("radio", "Single choice"),
    CHECKBOX("checkbox", "Multiple choice"),
    RATING("rating", "Rating (1-5)");

    val needsOptions: Boolean get() = this == SELECT || this == RADIO || this == CHECKBOX

    companion object {
        fun fromValue(value: String) = entries.firstOrNull { it.value == value } ?: SHORT_TEXT
    }
}

data class ToolField(
    val id: String,
    val label: String,
    val type: ToolFieldType,
    val required: Boolean,
    val options: List<String>? = null,
    val placeholder: String? = null
)

/** A shareable registration form a teacher builds to collect new-learner details — mirrors `OnboardingForm` in the web app's types/tools.ts. */
data class OnboardingForm(
    val id: String,
    val title: String,
    val description: String?,
    val slug: String,
    val welcomeMessage: String?,
    val fields: List<ToolField>,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
) {
    val shareUrl: String get() = "${BuildConfig.SHARE_BASE_URL}/onboarding/$slug"
}

data class OnboardingSubmission(
    val id: String,
    val formId: String,
    val name: String?,
    val email: String?,
    val phone: String?,
    val answers: Map<String, String>,
    val createdAt: String
)

/**
 * Drops fields whose answer already appears in a submission's dedicated identity columns
 * (name/email/phone) — e.g. the default "Full name"/"Email address"/"Phone number" fields every new
 * Onboarding/Feedback form starts with. Without this, submission lists and CSV/PDF exports show the
 * same value twice: once via the dedicated column, once via this field's own answer.
 */
fun List<ToolField>.excludingIdentityFields(): List<ToolField> = filterNot { field ->
    field.type == ToolFieldType.EMAIL ||
        field.type == ToolFieldType.PHONE ||
        (field.type == ToolFieldType.SHORT_TEXT && field.label.trim().let { it.equals("name", ignoreCase = true) || it.equals("full name", ignoreCase = true) })
}

fun ToolFieldDto.toDomain(): ToolField = ToolField(
    id = id,
    label = label,
    type = ToolFieldType.fromValue(type),
    required = required,
    options = options,
    placeholder = placeholder
)

fun ToolField.toDto(): ToolFieldDto = ToolFieldDto(
    id = id,
    label = label,
    type = type.value,
    required = required,
    options = options,
    placeholder = placeholder
)

fun OnboardingFormDto.toDomain(): OnboardingForm = OnboardingForm(
    id = id,
    title = title,
    description = description,
    slug = slug,
    welcomeMessage = welcomeMessage,
    fields = fields.map { it.toDomain() },
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun OnboardingSubmissionDto.toDomain(): OnboardingSubmission = OnboardingSubmission(
    id = id,
    formId = formId,
    name = name,
    email = email,
    phone = phone,
    answers = answers.mapValues { (_, value) -> value.toDisplayText() },
    createdAt = createdAt
)

/** Flattens an arbitrary jsonb answer value (string | number | string[] | null) into display text — shared across all six Tools' submission/response models. */
internal fun JsonElement.toDisplayText(): String = when (this) {
    is JsonNull -> "—"
    is JsonArray -> joinToString(", ") { it.jsonPrimitive.content }
    is JsonPrimitive -> content
    is JsonObject -> toString()
}
