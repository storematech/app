package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionDifficulty
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
 * (which holds the Gemini key server-side — see that function's source for why). This repository
 * never touches the AI key itself; it just proxies the request and, on success, writes the
 * returned questions into the user's own question bank. Turning those questions into an actual
 * quiz is a separate, explicit step the user takes in the normal Create Quiz flow (with the AI
 * questions pre-selected), so they always review/title/configure before anything is created.
 */
@Singleton
class AiQuizRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val questionRepository: QuestionRepository
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateQuestionsFromPrompt(userId: String, prompt: String, questionCount: Int): AppResult<List<Question>> =
        generate(userId, prompt, questionCount, pdfBase64 = null, images = null)

    /** [pdfBase64] is the raw file's base64 — never persisted, only relayed through the edge function to Gemini. */
    suspend fun generateQuestionsFromPdf(userId: String, extraPrompt: String, pdfBase64: String, questionCount: Int): AppResult<List<Question>> =
        generate(userId, extraPrompt, questionCount, pdfBase64 = pdfBase64, images = null)

    /** [images] are (base64, mimeType) pairs for up to a few photos — never persisted, only relayed to Gemini. */
    suspend fun generateQuestionsFromImages(
        userId: String,
        extraPrompt: String,
        images: List<Pair<String, String>>,
        questionCount: Int
    ): AppResult<List<Question>> = generate(
        userId,
        extraPrompt,
        questionCount,
        pdfBase64 = null,
        images = images.map { (base64, mimeType) -> GenerateQuizAiImageInput(mimeType = mimeType, data = base64) }
    )

    private suspend fun generate(
        userId: String,
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

        val createdQuestions = questions.mapNotNull { aiQuestion ->
            val optionPairs = aiQuestion.options.map { it.text to it.isCorrect }
            if (optionPairs.size < 2 || optionPairs.none { it.second }) return@mapNotNull null

            val created = questionRepository.createQuestion(
                userId = userId,
                text = aiQuestion.text,
                type = QuestionType.SINGLE_CHOICE,
                points = 1.0,
                difficulty = QuestionDifficulty.MEDIUM,
                explanation = null,
                tags = listOf("AI Generated"),
                options = optionPairs,
                freeTextAnswer = null,
                imageUrl = null,
                isUngraded = false
            )
            (created as? AppResult.Success)?.data
        }

        if (createdQuestions.isEmpty()) {
            error("AI generated questions, but none could be saved. Please try again.")
        }

        createdQuestions
    }
}
