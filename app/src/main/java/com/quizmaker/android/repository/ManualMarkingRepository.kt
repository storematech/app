package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.MarkingItem
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.data.remote.dto.QuizAnswerDetailDto
import com.quizmaker.android.data.remote.dto.QuizResponseDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Free-text and fill-in-the-blank answers can't be auto-graded, so any such question that isn't
 * explicitly marked "Ungraded" (see the checkbox next to Points in QuestionEditSheet/CreateQuizScreen)
 * needs a human to award points after the fact. This repository finds every such answer across a
 * quiz's responses, and lets the teacher submit a mark for one.
 */
@Singleton
class ManualMarkingRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val quizRepository: QuizRepository
) {
    /** Every free-text/fill-in-the-blank answer needing (or already carrying) a manual mark, across
     *  every completed response to [quizId] — empty if the quiz has no graded questions of either
     *  type at all, or no completed responses yet. Pending ones (never marked) sort first. */
    suspend fun getMarkingItems(quizId: String): AppResult<List<MarkingItem>> = safeCall {
        val questions = when (val result = quizRepository.getQuestionsForQuiz(quizId)) {
            is AppResult.Success -> result.data
            is AppResult.Error -> error(result.message)
        }
        // Widened from FREE_TEXT-only: FILL_IN_BLANK answers can't be auto-graded either (only
        // SINGLE_CHOICE/MULTI_CHOICE grade themselves via options[].isCorrect), and previously never
        // surfaced here at all — a confirmed dead end for any quiz relying on that question type,
        // including scanned OMR papers (see OmrRepository), where a fill-in-the-blank question always
        // has no bubbles and needs the exact same manual-marking flow as a Free Text one.
        val gradedFreeText = questions.filter {
            (it.type == QuestionType.FREE_TEXT || it.type == QuestionType.FILL_IN_BLANK) && !it.isUngraded
        }
        if (gradedFreeText.isEmpty()) return@safeCall emptyList()

        val maxPointsByQuestion = gradedFreeText.associate { it.id to it.points }
        val questionIds = gradedFreeText.map { it.id }

        val responses = supabase.from("quiz_responses")
            .select { filter { eq("quiz_id", quizId); eq("completed", true) } }
            .decodeList<QuizResponseDto>()
        if (responses.isEmpty()) return@safeCall emptyList()

        val responseIds = responses.map { it.id }
        val participantByResponse = responses.associate { r ->
            r.id to (r.userName?.trim()?.ifBlank { null } ?: r.userEmail)
        }

        val details = supabase.from("quiz_answer_details")
            .select {
                filter {
                    isIn("response_id", responseIds)
                    isIn("question_id", questionIds)
                }
            }
            .decodeList<QuizAnswerDetailDto>()

        details.mapNotNull { d ->
            val questionId = d.questionId ?: return@mapNotNull null
            val maxPoints = maxPointsByQuestion[questionId] ?: return@mapNotNull null
            MarkingItem(
                answerDetailId = d.id,
                responseId = d.responseId,
                questionId = questionId,
                questionText = d.questionText.orEmpty(),
                studentAnswer = d.answer?.trim()?.ifBlank { null } ?: "No answer",
                participantLabel = participantByResponse[d.responseId] ?: "Unknown",
                maxPoints = maxPoints,
                pointsEarned = d.pointsEarned ?: 0.0,
                isPending = (d.status ?: "ungraded") == "ungraded"
            )
        }.sortedByDescending { it.isPending }
    }

    /**
     * Records [pointsEarned] for one answer, then recomputes and updates that response's overall
     * `quiz_responses.score` (a 0-100 percentage, same convention as everywhere else) from ALL of
     * its `quiz_answer_details.points_earned` rows — not just the one just marked, since a single
     * response can have several free-text answers awaiting marks.
     */
    suspend fun submitMarking(
        quizId: String,
        responseId: String,
        answerDetailId: String,
        pointsEarned: Double,
        questionMaxPoints: Double
    ): AppResult<Unit> = safeCall {
        val status = when {
            pointsEarned <= 0.0 -> "incorrect"
            pointsEarned >= questionMaxPoints -> "correct"
            else -> "partial"
        }
        supabase.from("quiz_answer_details").update({
            set("points_earned", pointsEarned)
            set("is_correct", pointsEarned >= questionMaxPoints)
            set("status", status)
        }) {
            filter { eq("id", answerDetailId) }
        }

        val quiz = when (val result = quizRepository.getQuizById(quizId)) {
            is AppResult.Success -> result.data
            is AppResult.Error -> error(result.message)
        }
        val allDetailsForResponse = supabase.from("quiz_answer_details")
            .select { filter { eq("response_id", responseId) } }
            .decodeList<QuizAnswerDetailDto>()
        val totalEarned = allDetailsForResponse.sumOf { it.pointsEarned ?: 0.0 }.coerceAtLeast(0.0)
        val scorePercent = if (quiz.maxPoints > 0) {
            (totalEarned * 100.0 / quiz.maxPoints).roundToInt().coerceAtLeast(0)
        } else {
            0
        }

        supabase.from("quiz_responses").update({ set("score", scorePercent) }) {
            filter { eq("id", responseId) }
        }
        Unit
    }
}
