package com.quizmaker.android.data.model

import com.quizmaker.android.data.remote.dto.ClassAiSummaryDto
import com.quizmaker.android.data.remote.dto.ClassDto
import kotlin.math.roundToInt

/** A group of quizzes — lets a teacher see combined performance instead of checking each quiz separately. */
data class QuizClass(
    val id: String,
    val name: String,
    val description: String?,
    val createdAt: String?
)

data class ClassSummary(
    val totalStudents: Int,
    val totalQuizzes: Int,
    val totalSubmissions: Int,
    val averageScore: Int,
    val averageTimeSeconds: Int?
)

data class ClassQuizPerformance(
    val quizId: String,
    val quizTitle: String,
    val participantCount: Int,
    val submissionCount: Int,
    val averageScore: Int,
    val averageTimeSeconds: Int?,
    val completionRate: Int
)

data class WeakLearner(
    val studentName: String,
    val studentEmail: String,
    val quizTitle: String,
    val score: Int
)

/**
 * Cached SQL aggregate across every quiz in a class, shown to users as "Class AI Summary" — same
 * rule as [QuizAiSummary]: no AI/ML involved, see get_class_ai_summary in supabase/sql/classes.sql.
 */
data class ClassAiSummary(
    val classId: String,
    val quizCount: Int,
    val participantCount: Int,
    val averageScore: Int,
    val bestQuizTitle: String?,
    val bestQuizScore: Int?,
    val worstQuizTitle: String?,
    val worstQuizScore: Int?
)

fun ClassDto.toDomain(): QuizClass = QuizClass(
    id = id,
    name = name,
    description = description,
    createdAt = createdAt
)

fun ClassAiSummaryDto.toDomain(): ClassAiSummary = ClassAiSummary(
    classId = classId,
    quizCount = quizCount,
    participantCount = participantCount,
    averageScore = averageScore.roundToInt(),
    bestQuizTitle = bestQuizTitle,
    bestQuizScore = bestQuizScore?.roundToInt(),
    worstQuizTitle = worstQuizTitle,
    worstQuizScore = worstQuizScore?.roundToInt()
)
