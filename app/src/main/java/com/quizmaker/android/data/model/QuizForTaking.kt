package com.quizmaker.android.data.model

/** Bundle returned by QuizTakingRepository.getQuizByShareId() — a public quiz plus its ordered questions. */
data class QuizForTaking(
    val quiz: Quiz,
    val questions: List<Question>
)
