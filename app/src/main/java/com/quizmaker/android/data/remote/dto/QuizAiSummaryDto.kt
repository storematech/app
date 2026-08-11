package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape returned by the `get_quiz_ai_summary` RPC (backed by `quiz_ai_summaries`). */
@Serializable
data class QuizAiSummaryDto(
    @SerialName("quiz_id") val quizId: String,
    @SerialName("participant_count") val participantCount: Int,
    @SerialName("average_score") val averageScore: Double,
    @SerialName("median_score") val medianScore: Double,
    @SerialName("avg_correct_count") val avgCorrectCount: Double,
    @SerialName("avg_wrong_count") val avgWrongCount: Double,
    @SerialName("avg_time_seconds") val avgTimeSeconds: Int? = null,
    @SerialName("confidence_level") val confidenceLevel: String,
    @SerialName("computed_at") val computedAt: String
)
