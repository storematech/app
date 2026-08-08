package com.quizmaker.android.data.model

import com.quizmaker.android.BuildConfig
import com.quizmaker.android.data.remote.dto.QuizDto
import kotlinx.datetime.Instant

/** Domain-level quiz — mirrors the `Quiz` interface in the web app's src/types/index.ts (Phase 1 subset). */
data class Quiz(
    val id: String,
    val title: String,
    val description: String,
    val createdBy: String,
    val shareId: String,
    val isClosed: Boolean,
    val showLeaderboard: Boolean,
    val allowMultipleAttempts: Boolean,
    val requireOtpVerification: Boolean,
    val timeLimit: Int?,
    val timeLimitType: String,
    val timePerQuestion: Int?,
    val shuffleQuestions: Boolean,
    val showResults: Boolean,
    val sendResultEmail: Boolean,
    val allowResultPdf: Boolean,
    val showContactDetails: Boolean,
    val instructions: String?,
    val collectEmail: Boolean,
    val collectAddress: Boolean,
    val collectPhone: Boolean,
    val quizColor: String,
    val maxPoints: Int,
    val createdAt: Instant?
) {
    val shareUrl: String get() = "${BuildConfig.SHARE_BASE_URL}/take-quiz/$shareId"
}

/** Bundles the fields collected across the Create Quiz stepper into one payload for QuizRepository.createQuiz(). */
data class NewQuizSpec(
    val title: String,
    val description: String?,
    val timeLimit: Int?,
    val timeLimitType: String,
    val timePerQuestion: Int?,
    val shuffleQuestions: Boolean,
    val showResults: Boolean,
    val sendResultEmail: Boolean,
    val allowResultPdf: Boolean,
    val showLeaderboard: Boolean,
    val showContactDetails: Boolean,
    val instructions: String?,
    val quizColor: String,
    val collectEmail: Boolean,
    val collectAddress: Boolean,
    val collectPhone: Boolean,
    val requireOtpVerification: Boolean,
    val allowMultipleAttempts: Boolean
)

fun QuizDto.toDomain(): Quiz = Quiz(
    id = id,
    title = title,
    description = description.orEmpty(),
    createdBy = createdBy.orEmpty(),
    shareId = shareId.orEmpty(),
    isClosed = isClosed ?: false,
    showLeaderboard = showLeaderboard ?: false,
    allowMultipleAttempts = allowMultipleAttempts ?: true,
    requireOtpVerification = requireOtpVerification ?: false,
    timeLimit = timeLimit,
    timeLimitType = timeLimitType ?: "overall",
    timePerQuestion = timePerQuestion,
    shuffleQuestions = shuffleQuestions ?: false,
    showResults = showResults ?: true,
    sendResultEmail = sendResultEmail ?: true,
    allowResultPdf = allowResultPdf ?: true,
    showContactDetails = showContactDetails ?: false,
    instructions = instructions,
    collectEmail = collectEmail ?: true,
    collectAddress = collectAddress ?: false,
    collectPhone = collectPhone ?: false,
    quizColor = quizColor ?: "#8b5cf6",
    maxPoints = maxPoints ?: 0,
    createdAt = createdAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
)
