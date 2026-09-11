package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.OmrLayout
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.data.remote.dto.OmrSheetDto
import com.quizmaker.android.data.remote.dto.OmrSheetInsertDto
import com.quizmaker.android.data.remote.dto.QuizAnswerDetailInsertDto
import com.quizmaker.android.data.remote.dto.QuizResponseDto
import com.quizmaker.android.data.remote.dto.QuizResponseInsertDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlin.math.roundToInt
import kotlin.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One question's answer as reconstructed/graded by the (future, part-2) OpenCV scanning engine from
 * a photographed OMR sheet — this repository only persists it, it never computes it. Mirrors the
 * shape of [com.quizmaker.android.repository.GradedAnswer] (QuizTakingRepository's own online-quiz
 * equivalent) closely enough that [submitScannedResponse] can insert it the exact same way.
 */
data class OmrGradedAnswer(
    val questionId: String,
    val questionText: String,
    val questionType: QuestionType,
    val answerLabel: String?,
    val selectedOptionId: String?,
    val selectedOptionText: String?,
    val correctOptionId: String?,
    val correctOptionText: String?,
    val isCorrect: Boolean,
    val pointsEarned: Double,
    val status: String
)

/**
 * Persists/reads the printable OMR sheet layout for a quiz (`omr_sheets` table — see
 * supabase/migrations/20260910120000_add_omr_sheets.sql) and submits a graded response reconstructed
 * from a scanned paper sheet, following the exact same two-step insert pattern
 * [QuizTakingRepository.submitQuizResponse] already uses for an online submission: insert one
 * `quiz_responses` row (selecting it back to get the generated id), then bulk-insert its
 * `quiz_answer_details` rows against that id.
 */
@Singleton
class OmrRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val quizRepository: QuizRepository
) {
    suspend fun saveLayout(quizId: String, userId: String, layout: OmrLayout): AppResult<Unit> = safeCall {
        supabase.from("omr_sheets").insert(
            OmrSheetInsertDto(
                quizId = quizId,
                createdBy = userId,
                pageWidth = layout.pageWidth,
                pageHeight = layout.pageHeight,
                layout = layout
            )
        )
        Unit
    }

    /** The most recently generated sheet layout for [quizId], or null if none has been exported yet. */
    suspend fun getLatestLayout(quizId: String): AppResult<OmrLayout?> = safeCall {
        supabase.from("omr_sheets")
            .select {
                filter { eq("quiz_id", quizId) }
                order("created_at", Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<OmrSheetDto>()
            ?.layout
    }

    /**
     * Submits a response reconstructed from a scanned/graded paper sheet. [answers] is taken as
     * already graded (per-question `isCorrect`/`pointsEarned`/`status` already computed by the
     * scanning engine) — this only computes the response's overall `score` percentage, using the
     * SAME maxPoints convention as everywhere else in the app (sum of `points` over this quiz's
     * questions where `!isUngraded` — see TakeQuizViewModel.grade()), then inserts exactly like
     * [QuizTakingRepository.submitQuizResponse] does.
     */
    suspend fun submitScannedResponse(
        quizId: String,
        learnerLabel: String,
        learnerEmail: String,
        answers: List<OmrGradedAnswer>
    ): AppResult<Unit> = safeCall {
        val questions = when (val result = quizRepository.getQuestionsForQuiz(quizId)) {
            is AppResult.Success -> result.data
            is AppResult.Error -> error(result.message)
        }
        val maxPoints = questions.filter { !it.isUngraded }.sumOf { it.points }

        val totalScore = answers.sumOf { it.pointsEarned }.coerceAtLeast(0.0)
        val scorePercent = if (maxPoints > 0) (totalScore * 100.0 / maxPoints).roundToInt().coerceAtLeast(0) else 0

        val response = supabase.from("quiz_responses")
            .insert(
                QuizResponseInsertDto(
                    quizId = quizId,
                    userEmail = learnerEmail,
                    userName = learnerLabel,
                    phoneNumber = null,
                    score = scorePercent,
                    completed = true,
                    cancelled = false,
                    tabSwitches = 0,
                    device = "offline-scan",
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
                        questionType = answer.questionType.value,
                        answer = answer.answerLabel,
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
        Unit
    }
}
