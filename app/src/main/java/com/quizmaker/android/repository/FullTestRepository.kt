package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.FullTestChapter
import com.quizmaker.android.data.model.FullTestChapterConfig
import com.quizmaker.android.data.model.FullTestExamPattern
import com.quizmaker.android.data.model.FullTestGeneratedQuestion
import com.quizmaker.android.data.model.FullTestNegativeMarking
import com.quizmaker.android.data.model.FullTestSubject
import com.quizmaker.android.data.remote.dto.FullTestChapterConfigDto
import com.quizmaker.android.data.remote.dto.FullTestGenerateConfigDto
import com.quizmaker.android.data.remote.dto.FullTestGenerateRequest
import com.quizmaker.android.data.remote.dto.FullTestNegativeMarkingDto
import com.quizmaker.android.data.remote.dto.FullTestResearchRequest
import com.quizmaker.android.data.remote.dto.FullTestResponse
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

/** Result of a completed "generate" call — [failedChapters] is non-empty only when some chapters'
 *  batches failed after every AI provider (see the edge function's per-chapter partial-failure
 *  handling); the test itself is still usable with whatever chapters did succeed. */
data class FullTestGenerationResult(
    val quizTitle: String,
    val questions: List<FullTestGeneratedQuestion>,
    val failedChapters: List<String>
)

/**
 * Calls the `generate-full-test` Edge Function's two phases — [researchExam] asks the AI to
 * describe a named exam's real structure (question count, marks, duration, negative marking,
 * subject/chapter weightage and difficulty split), which the Configure step shows as editable
 * defaults; [generateFullTest] then takes the user's finalized (possibly edited) configuration and
 * generates the actual test, batched per chapter server-side. Like AiQuizRepository, this only
 * ever proposes questions — nothing is written to the question bank until the review step's
 * selections are confirmed (see FullTestViewModel.confirmSelection(), which reuses
 * AiQuizRepository.saveQuestions() for the actual persistence).
 */
@Singleton
class FullTestRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    // encodeDefaults = true matters here specifically: kotlinx.serialization otherwise omits any
    // field that equals its declared default from the *encoded* (outgoing) JSON — and "phase" on
    // both request DTOs always equals its default ("research"/"generate"), so without this it was
    // silently missing from every request body, and the edge function saw it as undefined.
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun researchExam(examName: String): AppResult<FullTestExamPattern> = safeCall {
        val response = supabase.functions.invoke("generate-full-test") {
            contentType(ContentType.Application.Json)
            // Research is a single AI call, but still one of up to four provider fallbacks server-side.
            timeout { requestTimeoutMillis = 60_000 }
            setBody(json.encodeToString(FullTestResearchRequest(examName = examName)))
        }
        val result = json.decodeFromString<FullTestResponse>(response.bodyAsText())
        val exam = result.exam
        if (!result.success || exam == null) {
            error(result.error ?: "Couldn't research that exam. Please try again.")
        }
        FullTestExamPattern(
            examName = exam.examName,
            totalQuestions = exam.totalQuestions,
            totalMarks = exam.totalMarks,
            durationMinutes = exam.durationMinutes,
            questionTypes = exam.questionTypes,
            negativeMarking = exam.negativeMarking?.let { FullTestNegativeMarking(it.enabled, it.correctMarks, it.incorrectMarks) },
            subjects = exam.subjects.map { subject ->
                FullTestSubject(
                    name = subject.name,
                    chapters = subject.chapters.map {
                        FullTestChapter(
                            name = it.name,
                            weightagePercent = it.weightagePercent,
                            easyPercent = it.easyPercent,
                            mediumPercent = it.mediumPercent,
                            hardPercent = it.hardPercent
                        )
                    }
                )
            }
        )
    }

    /** [chapters]/[formats]/[negativeMarking] are exactly the user's finalized Configure-step
     *  state — this call has no knowledge of the original research response, only what's passed in. */
    suspend fun generateFullTest(
        examName: String,
        chapters: List<FullTestChapterConfig>,
        formats: List<String>,
        negativeMarking: FullTestNegativeMarking?
    ): AppResult<FullTestGenerationResult> = safeCall {
        val response = supabase.functions.invoke("generate-full-test") {
            contentType(ContentType.Application.Json)
            // Can fan out into dozens of per-chapter AI calls server-side — well beyond the
            // client-wide default, same reasoning as generate-quiz-ai's own override.
            timeout { requestTimeoutMillis = 280_000 }
            setBody(
                json.encodeToString(
                    FullTestGenerateRequest(
                        examName = examName,
                        config = FullTestGenerateConfigDto(
                            totalQuestions = chapters.sumOf { it.questionCount },
                            chapters = chapters.map {
                                FullTestChapterConfigDto(
                                    subject = it.subject,
                                    chapter = it.chapter,
                                    questionCount = it.questionCount,
                                    easyCount = it.easyCount,
                                    mediumCount = it.mediumCount,
                                    hardCount = it.hardCount
                                )
                            },
                            formats = formats,
                            negativeMarking = negativeMarking?.let {
                                FullTestNegativeMarkingDto(it.enabled, it.correctMarks, it.incorrectMarks)
                            }
                        )
                    )
                )
            )
        }
        val result = json.decodeFromString<FullTestResponse>(response.bodyAsText())
        val questions = result.questions
        if (!result.success || questions.isNullOrEmpty()) {
            error(result.error ?: "Couldn't generate that test. Please try again.")
        }
        FullTestGenerationResult(
            quizTitle = result.quizTitle?.trim()?.takeIf { it.isNotEmpty() } ?: "$examName Full Mock Test",
            questions = questions.map {
                FullTestGeneratedQuestion(
                    subject = it.subject,
                    chapter = it.chapter,
                    difficulty = it.difficulty,
                    format = it.format,
                    text = it.text,
                    options = it.options.map { opt -> opt.text to opt.isCorrect },
                    numericalAnswer = it.numericalAnswer,
                    explanation = it.explanation
                )
            },
            failedChapters = result.failedChapters.orEmpty()
        )
    }
}
