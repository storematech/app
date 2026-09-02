package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionDifficulty
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.remote.dto.OptionDto
import com.quizmaker.android.data.remote.dto.OptionInsertDto
import com.quizmaker.android.data.remote.dto.QuestionDto
import com.quizmaker.android.data.remote.dto.QuestionInsertDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/** Same per-question shape [QuestionRepository.createQuestion] takes, bundled up for a single
 *  bulk-insert call via [QuestionRepository.createQuestions]. */
data class QuestionInsertPayload(
    val text: String,
    val type: QuestionType,
    val points: Double,
    val negativePoints: Double = 0.0,
    val difficulty: QuestionDifficulty?,
    val explanation: String?,
    val tags: List<String>,
    val options: List<Pair<String, Boolean>>,
    val freeTextAnswer: String?,
    val imageUrl: String?,
    val isUngraded: Boolean
)

/**
 * Question bank CRUD — mirrors insertQuestionToSupabase()/updateQuestionInSupabase()/
 * getAllQuestionsFromSupabase() in the web app's supabaseService.ts.
 */
@Singleton
class QuestionRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getQuestionsForUser(userId: String): AppResult<List<Question>> = safeCall {
        supabase.from("questions")
            .select(Columns.raw("*, options(*)")) {
                filter { eq("created_by", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<QuestionDto>()
            .map { it.toDomain() }
    }

    suspend fun createQuestion(
        userId: String,
        text: String,
        type: QuestionType,
        points: Double,
        negativePoints: Double = 0.0,
        difficulty: QuestionDifficulty?,
        explanation: String?,
        tags: List<String>,
        options: List<Pair<String, Boolean>>,
        freeTextAnswer: String?,
        imageUrl: String?,
        isUngraded: Boolean
    ): AppResult<Question> = safeCall {
        val created = supabase.from("questions")
            .insert(
                QuestionInsertDto(
                    text = text,
                    type = type.value,
                    points = points,
                    negativePoints = negativePoints,
                    difficulty = difficulty?.value,
                    explanation = explanation,
                    level = null,
                    createdBy = userId,
                    tags = tags.ifEmpty { null },
                    answer = freeTextAnswer,
                    imageUrl = imageUrl,
                    isUngraded = isUngraded
                )
            ) { select() }
            .decodeSingle<QuestionDto>()

        val insertedOptions = if (options.isNotEmpty()) {
            supabase.from("options")
                .insert(options.map { (optionText, isCorrect) ->
                    OptionInsertDto(questionId = created.id, text = optionText, isCorrect = isCorrect)
                }) { select() }
                .decodeList<OptionDto>()
        } else emptyList()

        created.copy(options = insertedOptions).toDomain()
    }

    /**
     * Bulk equivalent of [createQuestion] — inserts every question in ONE request, then every
     * option (across all of them) in a SECOND request, instead of up to two requests PER
     * question. A Full Test batch can mean 75-250 questions, so the old one-at-a-time loop meant
     * up to ~500 sequential HTTP round trips to save a single batch — enough that a single
     * transient network blip (a plain connection reset, common enough on real networks) failed
     * the entire save, surfacing as a scary "high demand" error for what was really just one flaky
     * request out of hundreds. Relies on Postgrest/Postgres preserving input row order in a
     * multi-row INSERT...RETURNING, which is the standard, universally-relied-upon behavior for a
     * plain VALUES-list insert with no triggers involved.
     */
    suspend fun createQuestions(userId: String, questions: List<QuestionInsertPayload>): AppResult<List<Question>> = safeCall {
        if (questions.isEmpty()) return@safeCall emptyList()

        val createdQuestions = supabase.from("questions")
            .insert(
                questions.map { q ->
                    QuestionInsertDto(
                        text = q.text,
                        type = q.type.value,
                        points = q.points,
                        negativePoints = q.negativePoints,
                        difficulty = q.difficulty?.value,
                        explanation = q.explanation,
                        level = null,
                        createdBy = userId,
                        tags = q.tags.ifEmpty { null },
                        answer = q.freeTextAnswer,
                        imageUrl = q.imageUrl,
                        isUngraded = q.isUngraded
                    )
                }
            ) { select() }
            .decodeList<QuestionDto>()

        val allOptions = createdQuestions.zip(questions).flatMap { (created, original) ->
            original.options.map { (optionText, isCorrect) ->
                OptionInsertDto(questionId = created.id, text = optionText, isCorrect = isCorrect)
            }
        }

        val insertedOptions = if (allOptions.isNotEmpty()) {
            supabase.from("options").insert(allOptions) { select() }.decodeList<OptionDto>()
        } else emptyList()

        val optionsByQuestionId = insertedOptions.groupBy { it.questionId }
        createdQuestions.map { q -> q.copy(options = optionsByQuestionId[q.id].orEmpty()).toDomain() }
    }

    suspend fun updateQuestion(
        questionId: String,
        userId: String,
        text: String,
        type: QuestionType,
        points: Double,
        negativePoints: Double = 0.0,
        difficulty: QuestionDifficulty?,
        explanation: String?,
        tags: List<String>,
        options: List<Pair<String, Boolean>>,
        freeTextAnswer: String?,
        imageUrl: String?,
        isUngraded: Boolean
    ): AppResult<Question> = safeCall {
        supabase.from("questions")
            .update(
                QuestionInsertDto(
                    text = text,
                    type = type.value,
                    points = points,
                    negativePoints = negativePoints,
                    difficulty = difficulty?.value,
                    explanation = explanation,
                    level = null,
                    createdBy = userId,
                    tags = tags.ifEmpty { null },
                    answer = freeTextAnswer,
                    imageUrl = imageUrl,
                    isUngraded = isUngraded
                )
            ) { filter { eq("id", questionId) } }

        supabase.from("options").delete { filter { eq("question_id", questionId) } }
        if (options.isNotEmpty()) {
            supabase.from("options").insert(
                options.map { (optionText, isCorrect) ->
                    OptionInsertDto(questionId = questionId, text = optionText, isCorrect = isCorrect)
                }
            )
        }

        supabase.from("questions")
            .select(Columns.raw("*, options(*)")) { filter { eq("id", questionId) } }
            .decodeSingle<QuestionDto>()
            .toDomain()
    }

    suspend fun deleteQuestion(questionId: String): AppResult<Unit> = safeCall {
        supabase.from("options").delete { filter { eq("question_id", questionId) } }
        supabase.from("questions").delete { filter { eq("id", questionId) } }
        Unit
    }
}
