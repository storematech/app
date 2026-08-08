package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuizForTaking
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.remote.dto.OtpFunctionResponse
import com.quizmaker.android.data.remote.dto.QuizAnswerDetailInsertDto
import com.quizmaker.android.data.remote.dto.QuizDto
import com.quizmaker.android.data.remote.dto.QuizQuestionJoinDto
import com.quizmaker.android.data.remote.dto.QuizResponseDto
import com.quizmaker.android.data.remote.dto.QuizResponseInsertDto
import com.quizmaker.android.data.remote.dto.SendOtpRequest
import com.quizmaker.android.data.remote.dto.VerifyOtpRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Powers the guest "take quiz via share link" flow — mirrors getQuizByShareId(),
 * submitQuizResponseInSupabase(), and the send-otp/verify-otp Edge Function calls in the web app.
 * Phase 1 scope: public shareable-link quizzes only (no learner/group-gated access).
 */
@Singleton
class QuizTakingRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getQuizByShareId(shareId: String): AppResult<QuizForTaking> = safeCall {
        val quizDto = supabase.from("quizzes")
            .select { filter { eq("share_id", shareId) } }
            .decodeSingleOrNull<QuizDto>()
            ?: throw NoSuchElementException("This quiz link is invalid or the quiz has been removed.")

        val joined = supabase.from("quiz_questions")
            .select(Columns.raw("question_order, questions(*, options(*))")) {
                filter { eq("quiz_id", quizDto.id) }
                order("question_order", Order.ASCENDING)
            }
            .decodeList<QuizQuestionJoinDto>()

        val questions: List<Question> = joined
            .sortedBy { it.questionOrder }
            .map { it.questions.toDomain() }
            .let { list -> if (quizDto.shuffleQuestions == true) list.shuffled() else list }

        QuizForTaking(quiz = quizDto.toDomain(), questions = questions)
    }

    suspend fun hasCompletedQuiz(quizId: String, email: String): AppResult<Boolean> = safeCall {
        supabase.from("quiz_responses")
            .select {
                filter {
                    eq("quiz_id", quizId)
                    eq("user_email", email)
                    eq("completed", true)
                }
                limit(1)
            }
            .decodeList<QuizResponseDto>()
            .isNotEmpty()
    }

    suspend fun sendOtp(email: String, quizId: String, quizTitle: String): AppResult<Unit> = safeCall {
        val response = supabase.functions.invoke("send-otp") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SendOtpRequest(email = email, quizId = quizId, quizTitle = quizTitle)))
        }
        val result = json.decodeFromString<OtpFunctionResponse>(response.bodyAsText())
        if (!result.success) error(result.error ?: "Failed to send the verification code.")
    }

    suspend fun verifyOtp(email: String, quizId: String, otpCode: String): AppResult<Unit> = safeCall {
        val response = supabase.functions.invoke("verify-otp") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(VerifyOtpRequest(email = email, quizId = quizId, otpCode = otpCode)))
        }
        val result = json.decodeFromString<OtpFunctionResponse>(response.bodyAsText())
        if (!result.success) error(result.error ?: "Invalid or expired verification code.")
    }

    /**
     * Submits the response + per-question answer details, and returns the earned raw point total.
     *
     * `quiz_responses.score` stores a PERCENTAGE (0-100), not raw points — matching the web app's
     * own convention (see its leaderboard fallback: `Math.round((score/100)*maxPoints)`). [maxPoints]
     * is the sum of points across graded (non-ungraded) questions, used only to convert to that
     * percentage; the raw point total is still what's returned for the on-screen result.
     */
    suspend fun submitQuizResponse(
        quizId: String,
        userEmail: String,
        userName: String?,
        phoneNumber: String?,
        answers: List<GradedAnswer>,
        maxPoints: Int
    ): AppResult<Int> = safeCall {
        val totalScore = answers.sumOf { it.pointsEarned }
        val scorePercent = if (maxPoints > 0) (totalScore * 100.0 / maxPoints).roundToInt() else 0

        val response = supabase.from("quiz_responses")
            .insert(
                QuizResponseInsertDto(
                    quizId = quizId,
                    userEmail = userEmail,
                    userName = userName,
                    phoneNumber = phoneNumber,
                    score = scorePercent,
                    completed = true,
                    cancelled = false,
                    tabSwitches = 0,
                    device = "Android",
                    location = null,
                    completedAt = Clock.System.now().toString()
                )
            ) { select() }
            .decodeSingle<QuizResponseDto>()

        if (answers.isNotEmpty()) {
            supabase.from("quiz_answer_details").insert(
                answers.map { answer ->
                    QuizAnswerDetailInsertDto(
                        responseId = response.id,
                        questionId = answer.questionId,
                        questionText = answer.questionText,
                        questionType = answer.questionType,
                        answer = answer.answerText,
                        selectedOptionId = answer.selectedOptionId,
                        selectedOptionText = answer.selectedOptionText,
                        correctOptionId = answer.correctOptionId,
                        correctOptionText = answer.correctOptionText,
                        isCorrect = answer.isCorrect,
                        pointsEarned = answer.pointsEarned,
                        status = answer.status
                    )
                }
            )
        }

        totalScore
    }
}

/** One graded answer, computed client-side before submission. */
data class GradedAnswer(
    val questionId: String,
    val questionText: String,
    val questionType: String,
    val answerText: String?,
    val selectedOptionId: String?,
    val selectedOptionText: String?,
    val correctOptionId: String?,
    val correctOptionText: String?,
    val isCorrect: Boolean,
    val pointsEarned: Int,
    val status: String
)
