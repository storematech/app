package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape of the `groups` table. */
@Serializable
data class GroupDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
data class GroupInsertDto(
    val name: String,
    val description: String?,
    @SerialName("created_by") val createdBy: String
)

/** Nested `groups(name)` join used by the learners select below. */
@Serializable
data class LearnerGroupNameDto(val name: String? = null)

/**
 * Row shape of the `learners` table, narrowed to the columns this app actually reads (plus the
 * joined parent group name) — the web app's `learners` table also carries LMS-portal columns
 * (e.g. `has_lms_access`) that are irrelevant here, so selects are always scoped to this exact
 * column list rather than `*` to avoid an unknown-key decode failure.
 */
@Serializable
data class LearnerDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    val name: String,
    val email: String? = null,
    @SerialName("group_id") val groupId: String? = null,
    val groups: LearnerGroupNameDto? = null,
    @SerialName("parent_name") val parentName: String? = null,
    @SerialName("parent_contact") val parentContact: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class LearnerInsertDto(
    @SerialName("student_id") val studentId: String,
    val name: String,
    val email: String,
    @SerialName("group_id") val groupId: String?,
    @SerialName("parent_name") val parentName: String?,
    @SerialName("parent_contact") val parentContact: String?,
    val notes: String?,
    @SerialName("created_by") val createdBy: String
)

@Serializable
data class LearnerUpdateDto(
    val name: String,
    val email: String,
    @SerialName("group_id") val groupId: String?,
    @SerialName("parent_name") val parentName: String?,
    @SerialName("parent_contact") val parentContact: String?,
    val notes: String?
)

/** Row shape of `quiz_attempts`, narrowed the same way, with the quiz title joined in. */
@Serializable
data class QuizAttemptDto(
    val id: String,
    val score: Int? = null,
    @SerialName("total_questions") val totalQuestions: Int? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    val quizzes: QuizAttemptQuizTitleDto? = null
)

@Serializable
data class QuizAttemptQuizTitleDto(val title: String? = null)
