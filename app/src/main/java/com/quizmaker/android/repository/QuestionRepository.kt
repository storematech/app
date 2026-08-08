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
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

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
        points: Int,
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

    suspend fun updateQuestion(
        questionId: String,
        userId: String,
        text: String,
        type: QuestionType,
        points: Int,
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

    /** Count of `reported_questions` rows against this creator's own question bank (Dashboard's "Reported" tile). */
    suspend fun getReportedCount(questionIds: List<String>): AppResult<Int> = safeCall {
        if (questionIds.isEmpty()) return@safeCall 0
        supabase.from("reported_questions")
            .select {
                filter { isIn("question_id", questionIds) }
                count(Count.EXACT)
                head = true
            }
            .countOrNull()?.toInt() ?: 0
    }
}
