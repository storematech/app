package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.QuizDetailViewData
import com.quizmaker.android.data.model.QuizDetailViewSummary
import com.quizmaker.android.data.model.StudentAnswerRow
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

/**
 * Powers the "Quiz Detail View" screen — one row per respondent, mirroring the web app's detail
 * view table.
 *
 * Large quizzes (hundreds+ of responses) used to blow up: fetching every response then asking
 * `quiz_answer_details` for `response_id IN (...)` with all of them at once built a URL with
 * hundreds of UUIDs in the query string, which Supabase/PostgREST rejected outright. The fix is
 * to never build that IN-clause for more than a page (or export chunk) of responses at a time —
 * [getSummary] only touches the lightweight `quiz_responses` columns (no per-answer join) so it
 * scales to any response count, and [getPage]/[getAllRows] cap how many response ids ever land in
 * a single `quiz_answer_details` query.
 */
@Singleton
class QuizDetailViewRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        const val PAGE_SIZE = 20
        private const val ANSWER_DETAIL_CHUNK = 100
    }

    suspend fun getSummary(quizId: String): AppResult<QuizDetailViewSummary> = safeCall {
        val quiz = supabase.from("quizzes")
            .select { filter { eq("id", quizId) } }
            .decodeSingle<QuizDto>()

        val questions = supabase.from("quiz_questions")
            .select(Columns.raw("question_order, questions(*)")) {
                filter { eq("quiz_id", quizId) }
            }
            .decodeList<QuizQuestionJoinDto>()
            .map { it.questions.toDomain() }
        val gradedCount = questions.count { !it.isUngraded }

        val responses = supabase.from("quiz_responses")
            .select {
                filter { eq("quiz_id", quizId); eq("completed", true) }
            }
            .decodeList<QuizResponseDto>()

        val averageScore = if (responses.isEmpty()) 0 else {
            responses.map { it.score ?: 0 }.average().let { if (it.isNaN()) 0 else it.toInt() }
        }
        val durations = responses.mapNotNull { secondsBetween(it.startedAt, it.completedAt) }
        val averageTimeSeconds = if (durations.isEmpty()) null else durations.average().toInt()

        QuizDetailViewSummary(
            quizTitle = quiz.title,
            gradedCount = gradedCount,
            allowMultipleAttempts = quiz.allowMultipleAttempts ?: true,
            totalResponses = responses.size,
            averageScore = averageScore,
            averageTimeSeconds = averageTimeSeconds
        )
    }

    /** One page of respondent rows, newest first. [offset]/[limit] are response-level, not row-level. */
    suspend fun getPage(quizId: String, gradedCount: Int, offset: Int, limit: Int = PAGE_SIZE): AppResult<List<StudentAnswerRow>> = safeCall {
        val responses = supabase.from("quiz_responses")
            .select {
                filter { eq("quiz_id", quizId); eq("completed", true) }
                order("completed_at", Order.DESCENDING)
                range((offset).toLong(), (offset + limit - 1).toLong())
            }
            .decodeList<QuizResponseDto>()

        buildRows(responses, gradedCount)
    }

    /** Fetches every respondent row, chunking requests so the export path never hits the same URL-length wall. */
    suspend fun getAllRows(quizId: String, gradedCount: Int): AppResult<List<StudentAnswerRow>> = safeCall {
        val allRows = mutableListOf<StudentAnswerRow>()
        var offset = 0
        while (true) {
            val responses = supabase.from("quiz_responses")
                .select {
                    filter { eq("quiz_id", quizId); eq("completed", true) }
                    order("completed_at", Order.DESCENDING)
                    range(offset.toLong(), (offset + ANSWER_DETAIL_CHUNK - 1).toLong())
                }
                .decodeList<QuizResponseDto>()
            if (responses.isEmpty()) break

            allRows += buildRows(responses, gradedCount)
            if (responses.size < ANSWER_DETAIL_CHUNK) break
            offset += ANSWER_DETAIL_CHUNK
        }
        allRows
    }

    private suspend fun buildRows(responses: List<QuizResponseDto>, gradedCount: Int): List<StudentAnswerRow> {
        if (responses.isEmpty()) return emptyList()

        val correctByResponse: Map<String, Int> = supabase.from("quiz_answer_details")
            .select { filter { isIn("response_id", responses.map { it.id }) } }
            .decodeList<QuizAnswerDetailDto>()
            .filter { it.isCorrect == true }
            .groupingBy { it.responseId }
            .eachCount()

        return responses.map { r ->
            StudentAnswerRow(
                responseId = r.id,
                name = r.userName?.ifBlank { null } ?: r.userEmail.substringBefore("@"),
                email = r.userEmail,
                scorePercent = r.score ?: 0,
                correctCount = correctByResponse[r.id] ?: 0,
                gradedCount = gradedCount,
                startedAt = r.startedAt,
                completedAt = r.completedAt,
                timeSeconds = secondsBetween(r.startedAt, r.completedAt),
                attemptNumber = r.attemptNumber ?: 1
            )
        }
    }

    private fun secondsBetween(startIso: String?, endIso: String?): Int? {
        if (startIso == null || endIso == null) return null
        return runCatching {
            (Instant.parse(endIso) - Instant.parse(startIso)).inWholeSeconds.toInt().coerceAtLeast(0)
        }.getOrNull()
    }
}
