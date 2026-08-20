package com.quizmaker.android.data.model

import com.quizmaker.android.data.remote.dto.GroupDto
import com.quizmaker.android.data.remote.dto.LearnerDto
import com.quizmaker.android.data.remote.dto.LearnerQuizAttemptDto

/** A student/participant a teacher manages outside of any single quiz — see LearnersRepository. */
data class Learner(
    val id: String,
    val studentId: String,
    val name: String,
    val email: String?,
    val groupId: String?,
    val groupName: String?,
    val parentName: String?,
    val parentContact: String?,
    val notes: String?,
    val createdAt: String?
)

/** A group/batch of learners — lets a teacher filter and manage students together. */
data class Group(
    val id: String,
    val name: String,
    val description: String?,
    val createdAt: String?
)

/** A learner's past quiz attempt, shown on their Student Profile. */
data class LearnerQuizAttempt(
    val id: String,
    val quizTitle: String,
    val score: Int?,
    val totalQuestions: Int?,
    val completedAt: String?,
    val timeTaken: Int?
)

fun LearnerDto.toDomain(): Learner = Learner(
    id = id,
    studentId = studentId,
    name = name,
    email = email,
    groupId = groupId,
    groupName = groups?.name,
    parentName = parentName,
    parentContact = parentContact,
    notes = notes,
    createdAt = createdAt
)

fun GroupDto.toDomain(): Group = Group(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt
)

fun LearnerQuizAttemptDto.toDomain(): LearnerQuizAttempt = LearnerQuizAttempt(
    id = id,
    quizTitle = quizzes?.title ?: "Unknown Quiz",
    score = score,
    totalQuestions = totalQuestions,
    completedAt = completedAt,
    timeTaken = timeTaken
)
