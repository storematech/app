package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape of the `reported_questions` table — a learner's report against a question in one of this creator's quizzes. */
@Serializable
data class ReportedQuestionDto(
    val id: String,
    @SerialName("question_id") val questionId: String,
    @SerialName("question_text") val questionText: String,
    @SerialName("quiz_id") val quizId: String,
    @SerialName("quiz_title") val quizTitle: String,
    @SerialName("reported_by") val reportedBy: String,
    @SerialName("report_date") val reportDate: String? = null,
    val reason: String,
    val status: String = "pending",
    @SerialName("response_id") val responseId: String? = null
)

/** Partial update — only ever used to flip [ReportedQuestionDto.status] once a creator has reviewed a report. */
@Serializable
data class ReportedQuestionStatusUpdateDto(val status: String)
