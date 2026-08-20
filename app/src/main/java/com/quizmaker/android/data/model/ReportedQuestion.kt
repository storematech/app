package com.quizmaker.android.data.model

import com.quizmaker.android.data.remote.dto.ReportedQuestionDto
import kotlin.time.Instant

enum class ReportedQuestionStatus(val value: String) {
    PENDING("pending"),
    RESOLVED("resolved");

    companion object {
        fun from(value: String): ReportedQuestionStatus =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: PENDING
    }
}

/** A learner's report against a question in one of this creator's quizzes — mirrors the `reported_questions` table. */
data class ReportedQuestion(
    val id: String,
    val questionId: String,
    val questionText: String,
    val quizId: String,
    val quizTitle: String,
    val reportedBy: String,
    val reportDate: Instant?,
    val reason: String,
    val status: ReportedQuestionStatus,
    val responseId: String?
)

fun ReportedQuestionDto.toDomain(): ReportedQuestion = ReportedQuestion(
    id = id,
    questionId = questionId,
    questionText = questionText,
    quizId = quizId,
    quizTitle = quizTitle,
    reportedBy = reportedBy,
    reportDate = reportDate?.let { runCatching { Instant.parse(it) }.getOrNull() },
    reason = reason,
    status = ReportedQuestionStatus.from(status),
    responseId = responseId
)
