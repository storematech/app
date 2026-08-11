package com.quizmaker.android.data.model

import com.quizmaker.android.data.remote.dto.QuizAiSummaryDto
import kotlin.math.roundToInt

/**
 * Pre-aggregated quiz stats shown to users as "AI Summary" — despite the branding, every value
 * here is a plain SQL aggregate computed and cached server-side by the get_quiz_ai_summary
 * Postgres function (supabase/sql/quiz_ai_summary.sql). No AI/ML involved anywhere.
 */
enum class LearnerConfidence { NEGATIVE, NEUTRAL, POSITIVE }

data class QuizAiSummary(
    val quizId: String,
    val participantCount: Int,
    val averageScore: Int,
    val medianScore: Int,
    val avgCorrectCount: Double,
    val avgWrongCount: Double,
    val avgTimeSeconds: Int?,
    val confidence: LearnerConfidence
)

fun QuizAiSummaryDto.toDomain(): QuizAiSummary = QuizAiSummary(
    quizId = quizId,
    participantCount = participantCount,
    averageScore = averageScore.roundToInt(),
    medianScore = medianScore.roundToInt(),
    avgCorrectCount = avgCorrectCount,
    avgWrongCount = avgWrongCount,
    avgTimeSeconds = avgTimeSeconds,
    confidence = when (confidenceLevel) {
        "negative" -> LearnerConfidence.NEGATIVE
        "positive" -> LearnerConfidence.POSITIVE
        else -> LearnerConfidence.NEUTRAL
    }
)
