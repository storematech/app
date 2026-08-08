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

@Serializable
data class GenerateQuizAiOptionDto(val text: String, val isCorrect: Boolean)

@Serializable
data class GenerateQuizAiQuestionDto(val text: String, val options: List<GenerateQuizAiOptionDto>)

@Serializable
data class GenerateQuizAiResponse(
    val success: Boolean,
    val quizTitle: String? = null,
    val questions: List<GenerateQuizAiQuestionDto>? = null,
    val error: String? = null
)
