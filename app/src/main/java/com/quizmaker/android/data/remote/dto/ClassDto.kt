package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape of the `classes` table. */
@Serializable
data class ClassDto(
    val id: String,
    @SerialName("created_by") val createdBy: String? = null,
    val name: String,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ClassInsertDto(
    @SerialName("created_by") val createdBy: String,
    val name: String,
    val description: String?
)

/**
 * Row shape of the `class_quizzes` join table. Every field is optional/nullable — several call
 * sites in ClassRepository select just `quiz_id` or `class_id` (never `id` itself, which nothing
 * actually reads), and a non-nullable `id` there would fail to decode with a missing-field error.
 */
@Serializable
data class ClassQuizDto(
    val id: String? = null,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("quiz_id") val quizId: String? = null
)

@Serializable
data class ClassQuizInsertDto(
    @SerialName("class_id") val classId: String,
    @SerialName("quiz_id") val quizId: String
)

/** Row shape returned by the `get_class_ai_summary` RPC (backed by `class_ai_summaries`). */
@Serializable
data class ClassAiSummaryDto(
    @SerialName("class_id") val classId: String,
    @SerialName("quiz_count") val quizCount: Int,
    @SerialName("participant_count") val participantCount: Int,
    @SerialName("average_score") val averageScore: Double,
    @SerialName("best_quiz_title") val bestQuizTitle: String? = null,
    @SerialName("best_quiz_score") val bestQuizScore: Double? = null,
    @SerialName("worst_quiz_title") val worstQuizTitle: String? = null,
    @SerialName("worst_quiz_score") val worstQuizScore: Double? = null,
    @SerialName("computed_at") val computedAt: String? = null
)
