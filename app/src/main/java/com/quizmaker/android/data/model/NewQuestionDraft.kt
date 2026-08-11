package com.quizmaker.android.data.model

/** Shared "new/edit question" form state — driven by Question Bank's own "New" button and Revision's "edit" tap alike. */
data class NewQuestionDraft(
    val text: String = "",
    val type: QuestionType = QuestionType.SINGLE_CHOICE,
    val options: List<String> = listOf("", "", "", ""),
    val correctOptionIndex: Int = 0,
    val correctOptionIndices: Set<Int> = setOf(0),
    val freeTextAnswer: String = "",
    val points: Int = 1,
    val difficulty: QuestionDifficulty = QuestionDifficulty.MEDIUM,
    // Carried through from the source question when editing (no UI for these yet), so saving
    // an edit doesn't silently wipe them. Null id means "creating a new question".
    val editingQuestionId: String? = null,
    val tags: List<String> = emptyList(),
    val explanation: String? = null
)

/** Populates a draft for editing an existing question — used by both Question Bank and Revision. */
fun Question.toDraft(): NewQuestionDraft {
    val isChoice = type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTI_CHOICE
    val optionTexts = if (isChoice && options.isNotEmpty()) {
        options.map { it.text }
    } else {
        listOf("", "", "", "")
    }
    val correctIndex = options.indexOfFirst { it.isCorrect }.takeIf { it >= 0 } ?: 0
    val correctIndices = options.withIndex()
        .filter { (_, option) -> option.isCorrect }
        .map { (index, _) -> index }
        .toSet()
        .ifEmpty { setOf(0) }

    return NewQuestionDraft(
        text = text,
        type = type,
        options = optionTexts,
        correctOptionIndex = correctIndex,
        correctOptionIndices = correctIndices,
        freeTextAnswer = correctAnswer.orEmpty(),
        points = points,
        difficulty = difficulty ?: QuestionDifficulty.MEDIUM,
        editingQuestionId = id,
        tags = tags,
        explanation = explanation
    )
}
