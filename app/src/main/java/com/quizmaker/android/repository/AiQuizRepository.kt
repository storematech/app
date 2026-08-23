package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionDifficulty
import com.quizmaker.android.data.model.QuestionOption
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.data.remote.dto.GenerateQuizAiImageInput
import com.quizmaker.android.data.remote.dto.GenerateQuizAiRequest
import com.quizmaker.android.data.remote.dto.GenerateQuizAiResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.plugins.timeout
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates quiz questions from a prompt/PDF/photos via the `generate-quiz-ai` Edge Function
 * (which holds the Gemini key server-side — see that function's source for why). [generate] only
 * ever proposes questions for review — it writes nothing to the user's question bank. Each
 * returned [Question] carries a synthetic, client-only id (not a real row id), just stable enough
 * for the review step's checkbox selection to track. [saveQuestions] is the separate, explicit
 * step that actually persists the ones the user kept, called once they confirm — either into an
 * actual quiz (normal Create Quiz flow, pre-selected) or straight into the question bank (Add
 * Questions mode). Regenerating or backing out before that point leaves the question bank
 * untouched, same as any other user input they discarded without submitting.
 */
@Singleton
class AiQuizRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val questionRepository: QuestionRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateQuestionsFromPrompt(prompt: String, questionCount: Int): AppResult<List<Question>> =
        generate(prompt, questionCount, pdfBase64 = null, images = null)

    /** [pdfBase64] is the raw file's base64 — never persisted, only relayed through the edge function to Gemini. */
    suspend fun generateQuestionsFromPdf(extraPrompt: String, pdfBase64: String, questionCount: Int): AppResult<List<Question>> =
        generate(extraPrompt, questionCount, pdfBase64 = pdfBase64, images = null)

    /** [images] are (base64, mimeType) pairs for up to a few photos — never persisted, only relayed to Gemini. */
    suspend fun generateQuestionsFromImages(
        extraPrompt: String,
        images: List<Pair<String, String>>,
        questionCount: Int
    ): AppResult<List<Question>> = generate(
        extraPrompt,
        questionCount,
        pdfBase64 = null,
        images = images.map { (base64, mimeType) -> GenerateQuizAiImageInput(mimeType = mimeType, data = base64) }
    )

    private suspend fun generate(
        prompt: String,
        questionCount: Int,
        pdfBase64: String?,
        images: List<GenerateQuizAiImageInput>?
    ): AppResult<List<Question>> = safeCall {
        val response = supabase.functions.invoke("generate-quiz-ai") {
            contentType(ContentType.Application.Json)
            // The client-wide 30s requestTimeout (see SupabaseModule.kt) is fine for normal
            // queries, but this call now falls back through up to four AI providers server-side
            // before giving up — override just this request so it isn't cut off mid-chain.
            timeout { requestTimeoutMillis = 100_000 }
            setBody(
                json.encodeToString(
                    GenerateQuizAiRequest(prompt = prompt, questionCount = questionCount, pdfBase64 = pdfBase64, images = images)
                )
            )
        }
        val result = json.decodeFromString<GenerateQuizAiResponse>(response.bodyAsText())
        val questions = result.questions
        if (!result.success || questions.isNullOrEmpty()) {
            error(result.error ?: "Couldn't generate questions for that prompt. Try rephrasing it.")
        }

        val reviewQuestions = questions.mapIndexedNotNull { index, aiQuestion ->
            val options = aiQuestion.options.mapIndexed { optionIndex, option ->
                QuestionOption(id = "ai-review-$index-opt-$optionIndex", text = option.text, isCorrect = option.isCorrect)
            }
            if (options.size < 2 || options.none { it.isCorrect }) return@mapIndexedNotNull null

            Question(
                id = "ai-review-$index",
                text = aiQuestion.text,
                type = QuestionType.SINGLE_CHOICE,
                options = options,
                correctAnswer = null,
                explanation = null,
                difficulty = QuestionDifficulty.MEDIUM,
                tags = listOf("AI Generated"),
                points = 1.0,
                negativePoints = 0.0,
                imageUrl = null,
                isUngraded = false,
                createdAt = null
            )
        }

        if (reviewQuestions.isEmpty()) {
            error("AI generated questions, but none were usable. Please try again.")
        }

        reviewQuestions
    }

    /**
     * Persists the given (unsaved, synthetic-id) reviewed questions into [userId]'s question
     * bank, returning the real, saved rows. Fails the whole batch — rather than silently saving
     * a partial set — if any single question can't be created, so the caller never ends up with
     * an ambiguous "some of these are real, some aren't" result to reconcile.
     */
    suspend fun saveQuestions(userId: String, questions: List<Question>): AppResult<List<Question>> = safeCall {
        questions.map { question ->
            when (
                val created = questionRepository.createQuestion(
                    userId = userId,
                    text = question.text,
                    type = question.type,
                    points = question.points,
                    negativePoints = question.negativePoints,
                    difficulty = question.difficulty,
                    explanation = question.explanation,
                    tags = question.tags,
                    options = question.options.map { it.text to it.isCorrect },
                    freeTextAnswer = null,
                    imageUrl = question.imageUrl,
                    isUngraded = question.isUngraded
                )
            ) {
                is AppResult.Success -> created.data
                is AppResult.Error -> error(created.message)
            }
        }
    }
}
