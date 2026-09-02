package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.Serializable

/** Request/response shapes for the `generate-full-test` Supabase Edge Function's two phases. */

@Serializable
data class FullTestResearchRequest(val phase: String = "research", val examName: String)

@Serializable
data class FullTestGenerateRequest(val phase: String = "generate", val examName: String, val config: FullTestGenerateConfigDto)

@Serializable
data class FullTestGenerateConfigDto(
    val totalQuestions: Int,
    val chapters: List<FullTestChapterConfigDto>,
    val formats: List<String>,
    val negativeMarking: FullTestNegativeMarkingDto?
)

@Serializable
data class FullTestChapterConfigDto(
    val subject: String,
    val chapter: String,
    val questionCount: Int,
    val easyCount: Int,
    val mediumCount: Int,
    val hardCount: Int
)

@Serializable
data class FullTestNegativeMarkingDto(val enabled: Boolean, val correctMarks: Double, val incorrectMarks: Double)

@Serializable
data class FullTestResponse(
    val success: Boolean,
    val error: String? = null,
    // Research phase
    val exam: FullTestExamPatternDto? = null,
    // Generate phase
    val quizTitle: String? = null,
    val questions: List<FullTestGeneratedQuestionDto>? = null,
    val failedChapters: List<String>? = null
)

@Serializable
data class FullTestExamPatternDto(
    val examName: String,
    val totalQuestions: Int,
    val totalMarks: Int,
    val durationMinutes: Int,
    val questionTypes: List<String> = emptyList(),
    val negativeMarking: FullTestNegativeMarkingDto? = null,
    val subjects: List<FullTestSubjectDto> = emptyList()
)

@Serializable
data class FullTestSubjectDto(val name: String, val chapters: List<FullTestChapterDto> = emptyList())

@Serializable
data class FullTestChapterDto(
    val name: String,
    val weightagePercent: Int = 0,
    val easyPercent: Int = 0,
    val mediumPercent: Int = 0,
    val hardPercent: Int = 0
)

@Serializable
data class FullTestGeneratedQuestionDto(
    val subject: String,
    val chapter: String,
    val difficulty: String,
    val format: String,
    val text: String,
    val options: List<FullTestOptionDto> = emptyList(),
    val numericalAnswer: String? = null,
    val explanation: String? = null
)

@Serializable
data class FullTestOptionDto(val text: String, val isCorrect: Boolean)
