package com.quizmaker.android.data.model

import com.quizmaker.android.BuildConfig
import com.quizmaker.android.data.remote.dto.FeedbackFormDto
import com.quizmaker.android.data.remote.dto.FeedbackSubmissionDto

/** A shareable feedback form a teacher builds to collect ratings/comments after a session — mirrors `FeedbackForm` in the web app's types/tools.ts. Reuses [ToolField]/[ToolFieldType] from OnboardingTool.kt. */
data class FeedbackForm(
    val id: String,
    val title: String,
    val description: String?,
    val slug: String,
    val questions: List<ToolField>,
    val collectIdentity: Boolean,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
) {
    val shareUrl: String get() = "${BuildConfig.SHARE_BASE_URL}/feedback/$slug"
}

data class FeedbackSubmission(
    val id: String,
    val formId: String,
    val learnerName: String?,
    val learnerEmail: String?,
    val overallRating: Int?,
    val answers: Map<String, String>,
    val createdAt: String
)

fun FeedbackFormDto.toDomain(): FeedbackForm = FeedbackForm(
    id = id,
    title = title,
    description = description,
    slug = slug,
    questions = questions.map { it.toDomain() },
    collectIdentity = collectIdentity,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun FeedbackSubmissionDto.toDomain(): FeedbackSubmission = FeedbackSubmission(
    id = id,
    formId = formId,
    learnerName = learnerName,
    learnerEmail = learnerEmail,
    overallRating = overallRating,
    answers = answers.mapValues { (_, value) -> value.toDisplayText() },
    createdAt = createdAt
)
