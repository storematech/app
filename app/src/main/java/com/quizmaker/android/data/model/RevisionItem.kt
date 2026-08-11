package com.quizmaker.android.data.model

import com.quizmaker.android.data.remote.dto.RevisionItemJoinDto

enum class RevisionStatus { PENDING, DONE }

/** A question the user bookmarked "for revision" from the Question Performance screen. */
data class RevisionItem(
    val id: String,
    val question: Question,
    val quizId: String?,
    val quizTitle: String?,
    val status: RevisionStatus,
    val createdAt: String?
)

fun RevisionItemJoinDto.toDomain(): RevisionItem = RevisionItem(
    id = id,
    question = questions.toDomain(),
    quizId = quizId,
    quizTitle = quizzes?.title,
    status = if (status == "done") RevisionStatus.DONE else RevisionStatus.PENDING,
    createdAt = createdAt
)
