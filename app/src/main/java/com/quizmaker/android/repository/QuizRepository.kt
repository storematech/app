package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.NewQuizSpec
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.data.model.QuizResponse
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.remote.dto.QuestionDto
import com.quizmaker.android.data.remote.dto.QuizDto
import com.quizmaker.android.data.remote.dto.QuizInsertDto
import com.quizmaker.android.data.remote.dto.QuizQuestionInsertDto
import com.quizmaker.android.data.remote.dto.QuizQuestionJoinDto
import com.quizmaker.android.data.remote.dto.QuizResponseDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/** Minimal row shape for the count-batching queries below — only the `quiz_id` column is selected. */
@Serializable
private data class QuizIdRow(@SerialName("quiz_id") val quizId: String)

/**
 * Reads/writes the `quizzes` + `quiz_responses` tables for the signed-in creator.
 * Mirrors getQuizzesFromSupabase()/getQuizResponsesFromSupabase() in the web app's supabaseService.ts.
 */
@Singleton
class QuizRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        const val PAGE_SIZE = 20
    }

    suspend fun getQuizzesForUser(userId: String): AppResult<List<Quiz>> = safeCall {
        supabase.from("quizzes")
            .select {
                filter { eq("created_by", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<QuizDto>()
            .map { it.toDomain() }
    }

    /** One page of this user's quizzes, newest first — used by the Quiz List screen's infinite scroll. */
    suspend fun getQuizzesPage(userId: String, offset: Int, limit: Int = PAGE_SIZE): AppResult<List<Quiz>> = safeCall {
        supabase.from("quizzes")
            .select {
                filter { eq("created_by", userId) }
                order("created_at", Order.DESCENDING)
                range(offset.toLong(), (offset + limit - 1).toLong())
            }
            .decodeList<QuizDto>()
            .map { it.toDomain() }
    }

    /** Server-side title search, capped rather than paginated — search result sets are small in practice. */
    suspend fun searchQuizzesForUser(userId: String, query: String, limit: Int = 50): AppResult<List<Quiz>> = safeCall {
        supabase.from("quizzes")
            .select {
                filter { eq("created_by", userId); ilike("title", "%$query%") }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<QuizDto>()
            .map { it.toDomain() }
    }

    /** Mirrors the web app's two-step lookup: this user's quiz ids, then responses for those ids. */
    suspend fun getResponsesForUser(userId: String): AppResult<List<QuizResponse>> = safeCall {
        val quizIds = supabase.from("quizzes")
            .select { filter { eq("created_by", userId) } }
            .decodeList<QuizDto>()
            .map { it.id }

        if (quizIds.isEmpty()) return@safeCall emptyList()

        supabase.from("quiz_responses")
            .select { filter { isIn("quiz_id", quizIds) } }
            .decodeList<QuizResponseDto>()
            .map { it.toDomain() }
    }

    suspend fun closeQuiz(quizId: String): AppResult<Unit> = safeCall {
        supabase.from("quizzes").update({ set("is_closed", true) }) {
            filter { eq("id", quizId) }
        }
        Unit
    }

    suspend fun deleteQuiz(quizId: String): AppResult<Unit> = safeCall {
        supabase.from("quiz_questions").delete { filter { eq("quiz_id", quizId) } }
        supabase.from("quiz_responses").delete { filter { eq("quiz_id", quizId) } }
        supabase.from("quizzes").delete { filter { eq("id", quizId) } }
        Unit
    }

    suspend fun getQuizById(quizId: String): AppResult<Quiz> = safeCall {
        supabase.from("quizzes")
            .select { filter { eq("id", quizId) } }
            .decodeSingle<QuizDto>()
            .toDomain()
    }

    suspend fun getResponseCountForQuiz(quizId: String): AppResult<Int> = safeCall {
        supabase.from("quiz_responses")
            .select {
                filter { eq("quiz_id", quizId); eq("completed", true) }
                count(Count.EXACT)
                head = true
            }
            .countOrNull()?.toInt() ?: 0
    }

    /** Full questions (with options) linked to a quiz, in display order — used by the Master Paper export. */
    suspend fun getQuestionsForQuiz(quizId: String): AppResult<List<Question>> = safeCall {
        supabase.from("quiz_questions")
            .select(Columns.raw("question_order, questions(*, options(*))")) {
                filter { eq("quiz_id", quizId) }
                order("question_order", Order.ASCENDING)
            }
            .decodeList<QuizQuestionJoinDto>()
            .sortedBy { it.questionOrder }
            .map { it.questions.toDomain() }
    }

    suspend fun getQuestionCountForQuiz(quizId: String): AppResult<Int> = safeCall {
        supabase.from("quiz_questions")
            .select {
                filter { eq("quiz_id", quizId) }
                count(Count.EXACT)
                head = true
            }
            .countOrNull()?.toInt() ?: 0
    }

    /**
     * Batched counterpart to [getResponseCountForQuiz]/[getQuestionCountForQuiz] — one request per
     * table instead of one per quiz, used by the quiz list screen which otherwise fired 2 requests
     * per row (N+1) just to populate the "X questions / Y responses" chips.
     */
    suspend fun getQuestionCountsForQuizzes(quizIds: List<String>): AppResult<Map<String, Int>> = safeCall {
        if (quizIds.isEmpty()) return@safeCall emptyMap()
        supabase.from("quiz_questions")
            .select(Columns.raw("quiz_id")) { filter { isIn("quiz_id", quizIds) } }
            .decodeList<QuizIdRow>()
            .groupingBy { it.quizId }
            .eachCount()
    }

    suspend fun getResponseCountsForQuizzes(quizIds: List<String>): AppResult<Map<String, Int>> = safeCall {
        if (quizIds.isEmpty()) return@safeCall emptyMap()
        supabase.from("quiz_responses")
            .select(Columns.raw("quiz_id")) {
                filter { isIn("quiz_id", quizIds); eq("completed", true) }
            }
            .decodeList<QuizIdRow>()
            .groupingBy { it.quizId }
            .eachCount()
    }

    /**
     * Creates a quiz and links the selected question bank questions to it, mirroring
     * createQuiz()/addQuestionsToQuiz() in the web app's supabaseService.ts. Public
     * shareable-link quizzes only (Phase 1 scope — no learner/group visibility).
     */
    suspend fun createQuiz(userId: String, spec: NewQuizSpec, questionIds: List<String>): AppResult<Quiz> = safeCall {
        val created = supabase.from("quizzes")
            .insert(
                QuizInsertDto(
                    title = spec.title,
                    description = spec.description,
                    createdBy = userId,
                    isPublic = true,
                    visibilityType = "public",
                    timeLimit = spec.timeLimit,
                    timeLimitType = spec.timeLimitType,
                    timePerQuestion = spec.timePerQuestion,
                    shuffleQuestions = spec.shuffleQuestions,
                    showResults = spec.showResults,
                    sendResultEmail = spec.sendResultEmail,
                    allowResultPdf = spec.allowResultPdf,
                    showLeaderboard = spec.showLeaderboard,
                    showContactDetails = spec.showContactDetails,
                    instructions = spec.instructions,
                    theme = "standard",
                    quizColor = spec.quizColor,
                    collectEmail = spec.collectEmail,
                    collectAddress = spec.collectAddress,
                    collectPhone = spec.collectPhone,
                    requireOtpVerification = spec.requireOtpVerification,
                    allowMultipleAttempts = spec.allowMultipleAttempts,
                    shareId = generateShareId()
                )
            ) { select() }
            .decodeSingle<QuizDto>()

        if (questionIds.isNotEmpty()) {
            supabase.from("quiz_questions").insert(
                questionIds.mapIndexed { index, questionId ->
                    QuizQuestionInsertDto(quizId = created.id, questionId = questionId, questionOrder = index)
                }
            )

            val selectedQuestions = supabase.from("questions")
                .select { filter { isIn("id", questionIds) } }
                .decodeList<QuestionDto>()
            val maxPoints = selectedQuestions
                .filter { it.isUngraded != true }
                .sumOf { it.points ?: 1 }

            supabase.from("quizzes").update({ set("max_points", maxPoints) }) {
                filter { eq("id", created.id) }
            }
        }

        created.toDomain()
    }

    /** Updates a quiz's settings and replaces its linked questions — mirrors updateQuizInSupabase() in the web app. */
    suspend fun updateQuiz(quizId: String, spec: NewQuizSpec, questionIds: List<String>): AppResult<Quiz> = safeCall {
        val existing = supabase.from("quizzes")
            .select { filter { eq("id", quizId) } }
            .decodeSingle<QuizDto>()

        supabase.from("quizzes")
            .update(
                QuizInsertDto(
                    title = spec.title,
                    description = spec.description,
                    createdBy = existing.createdBy ?: "",
                    isPublic = existing.isPublic ?: true,
                    visibilityType = existing.visibilityType ?: "public",
                    timeLimit = spec.timeLimit,
                    timeLimitType = spec.timeLimitType,
                    timePerQuestion = spec.timePerQuestion,
                    shuffleQuestions = spec.shuffleQuestions,
                    showResults = spec.showResults,
                    sendResultEmail = spec.sendResultEmail,
                    allowResultPdf = spec.allowResultPdf,
                    showLeaderboard = spec.showLeaderboard,
                    showContactDetails = spec.showContactDetails,
                    instructions = spec.instructions,
                    theme = existing.theme ?: "standard",
                    quizColor = spec.quizColor,
                    collectEmail = spec.collectEmail,
                    collectAddress = spec.collectAddress,
                    collectPhone = spec.collectPhone,
                    requireOtpVerification = spec.requireOtpVerification,
                    allowMultipleAttempts = spec.allowMultipleAttempts,
                    shareId = existing.shareId ?: generateShareId()
                )
            ) { filter { eq("id", quizId) } }

        supabase.from("quiz_questions").delete { filter { eq("quiz_id", quizId) } }

        val maxPoints = if (questionIds.isNotEmpty()) {
            supabase.from("quiz_questions").insert(
                questionIds.mapIndexed { index, questionId ->
                    QuizQuestionInsertDto(quizId = quizId, questionId = questionId, questionOrder = index)
                }
            )
            supabase.from("questions")
                .select { filter { isIn("id", questionIds) } }
                .decodeList<QuestionDto>()
                .filter { it.isUngraded != true }
                .sumOf { it.points ?: 1 }
        } else {
            0
        }

        supabase.from("quizzes").update({ set("max_points", maxPoints) }) {
            filter { eq("id", quizId) }
        }

        supabase.from("quizzes")
            .select { filter { eq("id", quizId) } }
            .decodeSingle<QuizDto>()
            .toDomain()
    }

    /** Copies a quiz's settings and linked questions into a new quiz owned by the same creator. */
    suspend fun duplicateQuiz(quizId: String): AppResult<Quiz> = safeCall {
        val original = supabase.from("quizzes")
            .select { filter { eq("id", quizId) } }
            .decodeSingle<QuizDto>()

        val created = supabase.from("quizzes")
            .insert(
                QuizInsertDto(
                    title = "${original.title} (Copy)",
                    description = original.description,
                    createdBy = original.createdBy ?: "",
                    isPublic = original.isPublic ?: true,
                    visibilityType = original.visibilityType ?: "public",
                    timeLimit = original.timeLimit,
                    timeLimitType = original.timeLimitType ?: "total",
                    timePerQuestion = original.timePerQuestion,
                    shuffleQuestions = original.shuffleQuestions ?: false,
                    showResults = original.showResults ?: true,
                    sendResultEmail = original.sendResultEmail ?: false,
                    allowResultPdf = original.allowResultPdf ?: false,
                    showLeaderboard = original.showLeaderboard ?: true,
                    showContactDetails = original.showContactDetails ?: false,
                    instructions = original.instructions,
                    theme = original.theme ?: "standard",
                    quizColor = original.quizColor ?: "#6366f1",
                    collectEmail = original.collectEmail ?: false,
                    collectAddress = original.collectAddress ?: false,
                    collectPhone = original.collectPhone ?: false,
                    requireOtpVerification = original.requireOtpVerification ?: false,
                    allowMultipleAttempts = original.allowMultipleAttempts ?: true,
                    shareId = generateShareId()
                )
            ) { select() }
            .decodeSingle<QuizDto>()

        val originalQuestions = supabase.from("quiz_questions")
            .select {
                filter { eq("quiz_id", quizId) }
                order("question_order", Order.ASCENDING)
            }
            .decodeList<QuizQuestionInsertDto>()

        if (originalQuestions.isNotEmpty()) {
            supabase.from("quiz_questions").insert(
                originalQuestions.mapIndexed { index, qq ->
                    QuizQuestionInsertDto(quizId = created.id, questionId = qq.questionId, questionOrder = qq.questionOrder.takeIf { it >= 0 } ?: index)
                }
            )

            supabase.from("quizzes").update({ set("max_points", original.maxPoints ?: 0) }) {
                filter { eq("id", created.id) }
            }
        }

        created.toDomain()
    }

    private fun generateShareId(): String {
        val bytes = ByteArray(9)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
