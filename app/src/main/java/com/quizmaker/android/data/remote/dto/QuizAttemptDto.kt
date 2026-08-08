package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape of the `quiz_attempts` table (subset used for the Leaderboard's "time taken" column). */
@Serializable
data class QuizAttemptDto(
    @SerialName("student_email") val studentEmail: String? = null,
    @SerialName("time_taken") val timeTaken: Int? = null
)
