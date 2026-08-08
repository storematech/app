package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape of the `questions` table. */
@Serializable
data class QuestionDto(
    val id: String,
    val text: String,
    val type: String,
    val points: Int? = null,
    val difficulty: String? = null,
    val explanation: String? = null,
    val level: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val tags: List<String>? = null,
    val answer: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_ungraded") val isUngraded: Boolean? = null,
    // Only populated when selected via a join, e.g. questions(...options(*)) or quiz_questions!inner(questions(...)).
    val options: List<OptionDto>? = null
)

@Serializable
data class QuestionInsertDto(
    val text: String,
    val type: String,
    val points: Int,
    val difficulty: String?,
    val explanation: String?,
    val level: String?,
    @SerialName("created_by") val createdBy: String,
    val tags: List<String>?,
    val answer: String?,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("is_ungraded") val isUngraded: Boolean
)

/** Row shape of the `options` table. */
@Serializable
data class OptionDto(
    val id: String,
    @SerialName("question_id") val questionId: String? = null,
    val text: String,
    @SerialName("is_correct") val isCorrect: Boolean? = null
)

@Serializable
data class OptionInsertDto(
    @SerialName("question_id") val questionId: String,
    val text: String,
    @SerialName("is_correct") val isCorrect: Boolean
)

/** Row shape of the `quiz_questions` join table. */
@Serializable
data class QuizQuestionInsertDto(
    @SerialName("quiz_id") val quizId: String,
    @SerialName("question_id") val questionId: String,
    @SerialName("question_order") val questionOrder: Int
)

/** Shape of `quiz_questions` when embedding the related `questions` (+ its `options`) resource. */
@Serializable
data class QuizQuestionJoinDto(
    @SerialName("question_order") val questionOrder: Int,
    val questions: QuestionDto
)
