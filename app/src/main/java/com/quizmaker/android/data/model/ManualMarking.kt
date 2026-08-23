package com.quizmaker.android.data.model

/**
 * One free-text answer that needs (or has already received) a human-assigned score — only ever
 * built for questions where `type == FREE_TEXT` and `isUngraded == false`; anything auto-graded or
 * explicitly marked "Ungraded" never becomes one of these. See ManualMarkingRepository.
 */
data class MarkingItem(
    val answerDetailId: String,
    val responseId: String,
    val questionId: String,
    val questionText: String,
    val studentAnswer: String,
    /** The participant's name, falling back to their email — same convention as everywhere else
     *  a submission is attributed to someone. */
    val participantLabel: String,
    val maxPoints: Double,
    val pointsEarned: Double,
    /** True until a teacher submits a mark for this answer (see ManualMarkingRepository's KDoc for
     *  how this is derived from `quiz_answer_details.status`). */
    val isPending: Boolean
)
