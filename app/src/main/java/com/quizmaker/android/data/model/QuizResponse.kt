package com.quizmaker.android.data.model

import com.quizmaker.android.data.remote.dto.QuizResponseDto
import kotlinx.datetime.Instant

/** Domain-level quiz attempt/submission — mirrors `QuizResponse` in the web app's src/types/index.ts. */
data class QuizResponse(
    val id: String,
    val quizId: String,
    val userEmail: String,
    val userName: String?,
    val score: Int,
    val completed: Boolean,
    val cancelled: Boolean,
    val startedAt: Instant?,
    val completedAt: Instant?
) {
    val timeTakenSeconds: Int? get() {
        val start = startedAt ?: return null
        val end = completedAt ?: return null
        return (end - start).inWholeSeconds.toInt().coerceAtLeast(0)
    }
}

fun QuizResponseDto.toDomain(): QuizResponse = QuizResponse(
    id = id,
    quizId = quizId,
    userEmail = userEmail,
    userName = userName,
    score = score ?: 0,
    completed = completed ?: false,
    cancelled = cancelled ?: false,
    startedAt = startedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
    completedAt = completedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
)
