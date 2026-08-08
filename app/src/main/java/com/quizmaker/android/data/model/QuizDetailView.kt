package com.quizmaker.android.data.model

/** One respondent's row in the Quiz Detail View table. */
data class StudentAnswerRow(
    val responseId: String,
    val name: String,
    val email: String,
    val scorePercent: Int,
    val correctCount: Int,
    val gradedCount: Int,
    val startedAt: String?,
    val completedAt: String?,
    val timeSeconds: Int?,
    val attemptNumber: Int
)

data class QuizDetailViewData(
    val quizTitle: String,
    val gradedCount: Int,
    val allowMultipleAttempts: Boolean,
    val totalResponses: Int,
    val averageScore: Int,
    val averageTimeSeconds: Int?,
    val rows: List<StudentAnswerRow>
)

/** The lightweight, response-count-independent half of [QuizDetailViewData] — cheap to load even for huge quizzes. */
data class QuizDetailViewSummary(
    val quizTitle: String,
    val gradedCount: Int,
    val allowMultipleAttempts: Boolean,
    val totalResponses: Int,
    val averageScore: Int,
    val averageTimeSeconds: Int?
)
