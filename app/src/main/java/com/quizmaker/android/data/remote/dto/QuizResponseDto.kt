package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape of the `quiz_responses` table. */
@Serializable
data class QuizResponseDto(
    val id: String,
    @SerialName("quiz_id") val quizId: String,
    @SerialName("user_email") val userEmail: String,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val score: Int? = null,
    val completed: Boolean? = null,
    val cancelled: Boolean? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("tab_switches") val tabSwitches: Int? = null,
    @SerialName("cancellation_reason") val cancellationReason: String? = null,
    val device: String? = null,
    val location: String? = null,
    @SerialName("attempt_number") val attemptNumber: Int? = null
)

/** Payload for submitting a quiz attempt — mirrors submitQuizResponseInSupabase() in the web app. */
@Serializable
data class QuizResponseInsertDto(
    @SerialName("quiz_id") val quizId: String,
    @SerialName("user_email") val userEmail: String,
    @SerialName("user_name") val userName: String?,
    @SerialName("phone_number") val phoneNumber: String?,
    val score: Int,
    val completed: Boolean,
    val cancelled: Boolean,
    @SerialName("tab_switches") val tabSwitches: Int,
    val device: String?,
    val location: String?,
    @SerialName("completed_at") val completedAt: String?
)
