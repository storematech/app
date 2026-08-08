package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.QuestionStat
import com.quizmaker.android.data.model.QuizAnalysisData
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.remote.dto.QuizAnswerDetailDto
import com.quizmaker.android.data.remote.dto.QuizDto
import com.quizmaker.android.data.remote.dto.QuizQuestionJoinDto
import com.quizmaker.android.data.remote.dto.QuizResponseDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlin.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Powers the "Quiz Analysis" screen — per-question correct/wrong/skipped breakdown across all attempts. */
@Singleton
class QuizAnalysisRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getAnalysis(quizId: String): AppResult<QuizAnalysisData> = safeCall {
        val quiz = supabase.from("quizzes")
            .select { filter { eq("id", quizId) } }
            .decodeSingle<QuizDto>()

        val questions = supabase.from("quiz_questions")
            .select(Columns.raw("question_order, questions(*)")) {
                filter { eq("quiz_id", quizId) }
                order("question_order", Order.ASCENDING)
            }
            .decodeList<QuizQuestionJoinDto>()
            .sortedBy { it.questionOrder }
            .map { it.questions.toDomain() }

        val responses = supabase.from("quiz_responses")
            .select {
                filter { eq("quiz_id", quizId); eq("completed", true) }
            }
            .decodeList<QuizResponseDto>()

        if (responses.isEmpty()) {
            return@safeCall QuizAnalysisData(
                quizTitle = quiz.title,
                totalResponses = 0,
                averageScore = 0,
                averageTimeSeconds = null,
                questions = questions.mapIndexed { index, q ->
                    QuestionStat(
                        questionId = q.id, order = index + 1, text = q.text, type = q.type,
                        isUngraded = q.isUngraded, correctCount = 0, wrongCount = 0, skippedCount = 0
                    )
                }
            )
        }

        val details = supabase.from("quiz_answer_details")
            .select { filter { isIn("response_id", responses.map { it.id }) } }
            .decodeList<QuizAnswerDetailDto>()
            .filter { it.questionId != null }

        val detailsByQuestion = details.groupBy { it.questionId }

        val questionStats = questions.mapIndexed { index, q ->
            val forQuestion = detailsByQuestion[q.id].orEmpty()
            val correct = forQuestion.count { it.isCorrect == true }
            val wrong = forQuestion.count { it.isCorrect == false }
            QuestionStat(
                questionId = q.id,
                order = index + 1,
                text = q.text,
                type = q.type,
                isUngraded = q.isUngraded,
                correctCount = correct,
                wrongCount = wrong,
                skippedCount = (responses.size - correct - wrong).coerceAtLeast(0)
            )
        }

        val averageScore = responses.map { it.score ?: 0 }.average().let { if (it.isNaN()) 0 else it.toInt() }

        val durations = responses.mapNotNull { secondsBetween(it.startedAt, it.completedAt) }
        val averageTimeSeconds = if (durations.isEmpty()) null else durations.average().toInt()

        QuizAnalysisData(
            quizTitle = quiz.title,
            totalResponses = responses.size,
            averageScore = averageScore,
            averageTimeSeconds = averageTimeSeconds,
            questions = questionStats
        )
    }

    private fun secondsBetween(startIso: String?, endIso: String?): Int? {
        if (startIso == null || endIso == null) return null
        return runCatching {
            (Instant.parse(endIso) - Instant.parse(startIso)).inWholeSeconds.toInt().coerceAtLeast(0)
        }.getOrNull()
    }
}
