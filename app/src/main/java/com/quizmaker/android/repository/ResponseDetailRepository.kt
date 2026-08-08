package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.AnswerDetail
import com.quizmaker.android.data.model.AnswerStatus
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.data.model.ResponseDetailData
import com.quizmaker.android.data.remote.dto.QuestionDto
import com.quizmaker.android.data.remote.dto.QuizAnswerDetailDto
import com.quizmaker.android.data.remote.dto.QuizDto
import com.quizmaker.android.data.remote.dto.QuizResponseDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlin.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Powers the "Response Details" screen — one participant's full question-by-question breakdown. */
@Singleton
class ResponseDetailRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getResponseDetail(responseId: String): AppResult<ResponseDetailData> = safeCall {
        val response = supabase.from("quiz_responses")
            .select { filter { eq("id", responseId) } }
            .decodeSingle<QuizResponseDto>()

        val quiz = supabase.from("quizzes")
            .select { filter { eq("id", response.quizId) } }
            .decodeSingleOrNull<QuizDto>()

        val details = supabase.from("quiz_answer_details")
            .select { filter { eq("response_id", responseId) } }
            .decodeList<QuizAnswerDetailDto>()

        val questionIds = details.mapNotNull { it.questionId }
        val maxPointsByQuestion: Map<String, Int> = if (questionIds.isEmpty()) emptyMap() else {
            supabase.from("questions")
                .select { filter { isIn("id", questionIds) } }
                .decodeList<QuestionDto>()
                .associate { it.id to (it.points ?: 1) }
        }

        val answers = details.map { d ->
            val maxPoints = d.questionId?.let { maxPointsByQuestion[it] } ?: 1
            val earned = d.pointsEarned?.toInt() ?: 0
            AnswerDetail(
                id = d.id,
                questionText = d.questionText.orEmpty(),
                questionType = QuestionType.fromValue(d.questionType ?: "option"),
                studentAnswer = d.answer?.ifBlank { null } ?: d.selectedOptionText?.ifBlank { null } ?: "No answer",
                correctAnswer = d.correctOptionText?.ifBlank { null } ?: "N/A",
                pointsEarned = earned,
                maxPoints = maxPoints,
                status = statusFor(d.status, earned, maxPoints)
            )
        }

        ResponseDetailData(
            responseId = response.id,
            quizTitle = quiz?.title.orEmpty(),
            userName = response.userName?.ifBlank { null } ?: response.userEmail.substringBefore("@"),
            userEmail = response.userEmail,
            scorePercent = response.score ?: 0,
            startedAt = response.startedAt,
            completedAt = response.completedAt,
            timeSeconds = secondsBetween(response.startedAt, response.completedAt),
            answers = answers
        )
    }

    private fun statusFor(rawStatus: String?, earned: Int, maxPoints: Int): AnswerStatus = when (rawStatus) {
        "correct" -> AnswerStatus.CORRECT
        "partial" -> AnswerStatus.PARTIAL
        "incorrect" -> AnswerStatus.INCORRECT
        "ungraded" -> AnswerStatus.UNGRADED
        else -> when {
            earned <= 0 -> AnswerStatus.INCORRECT
            earned >= maxPoints -> AnswerStatus.CORRECT
            else -> AnswerStatus.PARTIAL
        }
    }

    private fun secondsBetween(startIso: String?, endIso: String?): Int? {
        if (startIso == null || endIso == null) return null
        return runCatching {
            (Instant.parse(endIso) - Instant.parse(startIso)).inWholeSeconds.toInt().coerceAtLeast(0)
        }.getOrNull()
    }
}
