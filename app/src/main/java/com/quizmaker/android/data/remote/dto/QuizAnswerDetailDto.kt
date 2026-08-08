package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Payload for one graded answer — mirrors the `quiz_answer_details` table / QuizAnswerDetail in the web app. */
@Serializable
data class QuizAnswerDetailInsertDto(
    @SerialName("response_id") val responseId: String,
    @SerialName("question_id") val questionId: String,
    @SerialName("question_text") val questionText: String,
    @SerialName("question_type") val questionType: String,
    val answer: String?,
    @SerialName("selected_option_id") val selectedOptionId: String?,
    @SerialName("selected_option_text") val selectedOptionText: String?,
    @SerialName("correct_option_id") val correctOptionId: String?,
    @SerialName("correct_option_text") val correctOptionText: String?,
    @SerialName("is_correct") val isCorrect: Boolean,
    @SerialName("points_earned") val pointsEarned: Int,
    val status: String
)

@Serializable
data class QuizAnswerDetailDto(
    val id: String,
    @SerialName("response_id") val responseId: String,
    @SerialName("question_id") val questionId: String? = null,
    @SerialName("question_text") val questionText: String? = null,
    @SerialName("question_type") val questionType: String? = null,
    val answer: String? = null,
    @SerialName("selected_option_text") val selectedOptionText: String? = null,
    @SerialName("correct_option_text") val correctOptionText: String? = null,
    @SerialName("is_correct") val isCorrect: Boolean? = null,
    // The `points_earned` column is `numeric` in Postgres, so PostgREST serializes it as "0.00" —
    // decoding that into an Int would throw, hence Double here.
    @SerialName("points_earned") val pointsEarned: Double? = null,
    val status: String? = null
)
