package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Payload for bookmarking a question — mirrors the `revision_items` table's insert columns. */
@Serializable
data class RevisionItemInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("question_id") val questionId: String,
    @SerialName("quiz_id") val quizId: String?
)

/** Shape of `revision_items` when embedding its related `questions` and `quizzes` resources. */
@Serializable
data class RevisionItemJoinDto(
    val id: String,
    @SerialName("quiz_id") val quizId: String? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String? = null,
    val questions: QuestionDto,
    val quizzes: RevisionQuizRefDto? = null
)

/** Just the bit of `quizzes` the Revision screen needs — the originating quiz's title, for its "filter by quiz" list. */
@Serializable
data class RevisionQuizRefDto(val title: String)
