package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.Serializable

/** Request/response shapes for the `generate-quiz-ai` Supabase Edge Function (proxies Gemini). */
@Serializable
data class GenerateQuizAiRequest(
    val prompt: String,
    val questionCount: Int,
    val pdfBase64: String? = null,
    val images: List<GenerateQuizAiImageInput>? = null
)

@Serializable
data class GenerateQuizAiImageInput(val mimeType: String, val data: String)

// isCorrect defaults to false (rather than being required) because Groq/OpenRouter/Cerebras are
// only told the JSON shape via a text prompt, not enforced server-side the way Gemini's structured
// output is — they occasionally omit this field on an option. A required Boolean here meant one
// missing isCorrect anywhere in the whole response threw a SerializationException and killed the
// entire parse (confirmed via a real PostHog-captured crash: "Field 'isCorrect' is required...
// missing at path: $.questions[0].options[0]"), discarding every other question too. Defaulting to
// false instead lets AiQuizRepository.generate()'s existing options.none { it.isCorrect } check
// handle it the same way it already handles a genuinely-all-wrong option set — dropping just that
// one question rather than crashing the whole batch.
@Serializable
data class GenerateQuizAiOptionDto(val text: String, val isCorrect: Boolean = false)

@Serializable
data class GenerateQuizAiQuestionDto(val text: String, val options: List<GenerateQuizAiOptionDto>)

@Serializable
data class GenerateQuizAiResponse(
    val success: Boolean,
    val quizTitle: String? = null,
    val questions: List<GenerateQuizAiQuestionDto>? = null,
    val error: String? = null
)
