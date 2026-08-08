package com.quizmaker.android.data.model

enum class AnswerStatus { CORRECT, PARTIAL, INCORRECT, UNGRADED }

data class AnswerDetail(
    val id: String,
    val questionText: String,
    val questionType: QuestionType,
    val studentAnswer: String,
    val correctAnswer: String,
    val pointsEarned: Int,
    val maxPoints: Int,
    val status: AnswerStatus
)

data class ResponseDetailData(
    val responseId: String,
    val quizTitle: String,
    val userName: String,
    val userEmail: String,
    val scorePercent: Int,
    val startedAt: String?,
    val completedAt: String?,
    val timeSeconds: Int?,
    val answers: List<AnswerDetail>
)
