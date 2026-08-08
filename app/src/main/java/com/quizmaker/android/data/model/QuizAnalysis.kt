package com.quizmaker.android.data.model

/** Per-question breakdown across all completed responses to a quiz. */
data class QuestionStat(
    val questionId: String,
    val order: Int,
    val text: String,
    val type: QuestionType,
    val isUngraded: Boolean,
    val correctCount: Int,
    val wrongCount: Int,
    val skippedCount: Int
) {
    val attempted: Int get() = correctCount + wrongCount
    val correctPercent: Int get() = if (attempted > 0) (correctCount * 100 / attempted) else 0
}

data class QuizAnalysisData(
    val quizTitle: String,
    val totalResponses: Int,
    val averageScore: Int,
    val averageTimeSeconds: Int?,
    val questions: List<QuestionStat>
)
