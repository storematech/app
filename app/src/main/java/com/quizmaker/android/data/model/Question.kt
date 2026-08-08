package com.quizmaker.android.data.model

import com.quizmaker.android.data.remote.dto.OptionDto
import com.quizmaker.android.data.remote.dto.QuestionDto

enum class QuestionType(val value: String) {
    SINGLE_CHOICE("option"),
    MULTI_CHOICE("multi-choice"),
    FREE_TEXT("free-text"),
    FILL_IN_BLANK("fill-in-the-blanks");

    companion object {
        fun fromValue(value: String) = entries.firstOrNull { it.value == value } ?: SINGLE_CHOICE
    }
}

enum class QuestionDifficulty(val value: String) {
    EASY("easy"), MEDIUM("medium"), HARD("hard")
}

data class QuestionOption(
    val id: String,
    val text: String,
    val isCorrect: Boolean
)

/** Domain-level question — mirrors `Question` in the web app's src/types/index.ts. */
data class Question(
    val id: String,
    val text: String,
    val type: QuestionType,
    val options: List<QuestionOption>,
    val correctAnswer: String?,
    val explanation: String?,
    val difficulty: QuestionDifficulty?,
    val tags: List<String>,
    val points: Int,
    val imageUrl: String?,
    val isUngraded: Boolean,
    val createdAt: String?
) {
    val correctOptionId: String? get() = options.firstOrNull { it.isCorrect }?.id
    val correctOptionIds: List<String> get() = options.filter { it.isCorrect }.map { it.id }
}

fun QuestionDto.toDomain(): Question = Question(
    id = id,
    text = text,
    type = QuestionType.fromValue(type),
    options = options.orEmpty().map { it.toDomain() },
    correctAnswer = answer,
    explanation = explanation,
    difficulty = difficulty?.let { d -> QuestionDifficulty.entries.firstOrNull { it.value == d } },
    tags = tags.orEmpty(),
    points = points ?: 1,
    imageUrl = imageUrl,
    isUngraded = isUngraded ?: false,
    createdAt = createdAt
)

fun OptionDto.toDomain(): QuestionOption = QuestionOption(
    id = id,
    text = text,
    isCorrect = isCorrect ?: false
)
