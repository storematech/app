package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.ClassAiSummary
import com.quizmaker.android.data.model.ClassQuizPerformance
import com.quizmaker.android.data.model.ClassSummary
import com.quizmaker.android.data.model.QuizClass
import com.quizmaker.android.data.model.WeakLearner
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.remote.dto.ClassAiSummaryDto
import com.quizmaker.android.data.remote.dto.ClassDto
import com.quizmaker.android.data.remote.dto.ClassInsertDto
import com.quizmaker.android.data.remote.dto.ClassQuizDto
import com.quizmaker.android.data.remote.dto.ClassQuizInsertDto
import com.quizmaker.android.data.remote.dto.QuizResponseDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.roundToInt
import kotlin.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Minimal row shape for narrow `quizzes` selects below — decoding into the full QuizDto would fail on missing columns. */
@Serializable
private data class QuizTitleRow(val id: String, val title: String)

/**
 * Reads/writes `classes`/`class_quizzes` and computes the live (uncached) Class Dashboard stats.
 * The Class AI Summary itself is the one cached/RPC-backed exception — see getClassAiSummary().
 */
@Singleton
class ClassRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getClasses(userId: String): AppResult<List<QuizClass>> = safeCall {
        supabase.from("classes")
            .select {
                filter { eq("created_by", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<ClassDto>()
            .map { it.toDomain() }
    }

    suspend fun getClassById(classId: String): AppResult<QuizClass> = safeCall {
        supabase.from("classes")
            .select { filter { eq("id", classId) } }
            .decodeSingle<ClassDto>()
            .toDomain()
    }

    suspend fun createClass(userId: String, name: String, description: String?): AppResult<QuizClass> = safeCall {
        supabase.from("classes")
            .insert(ClassInsertDto(createdBy = userId, name = name, description = description)) { select() }
            .decodeSingle<ClassDto>()
            .toDomain()
    }

    suspend fun getLinkedQuizIds(classId: String): AppResult<List<String>> = safeCall {
        fetchLinkedQuizIds(classId)
    }

    /** The reverse lookup — which classes a given quiz already belongs to, for the "Link to Class" sheet. */
    suspend fun getClassIdsForQuiz(quizId: String): AppResult<List<String>> = safeCall {
        supabase.from("class_quizzes")
            .select(Columns.raw("class_id")) { filter { eq("quiz_id", quizId) } }
            .decodeList<ClassQuizDto>()
            .mapNotNull { it.classId }
    }

    /** No-ops if already linked — `unique(class_id, quiz_id)` backs this, same idempotency shape as RevisionRepository.addForRevision. */
    suspend fun linkQuiz(classId: String, quizId: String): AppResult<Unit> = safeCall {
        val existing = supabase.from("class_quizzes")
            .select(Columns.raw("id")) {
                filter { eq("class_id", classId); eq("quiz_id", quizId) }
            }
            .decodeList<ClassQuizDto>()
        if (existing.isEmpty()) {
            supabase.from("class_quizzes").insert(ClassQuizInsertDto(classId = classId, quizId = quizId))
        }
        Unit
    }

    suspend fun unlinkQuiz(classId: String, quizId: String): AppResult<Unit> = safeCall {
        supabase.from("class_quizzes").delete {
            filter { eq("class_id", classId); eq("quiz_id", quizId) }
        }
        Unit
    }

    /** [since] is an ISO-8601 lower bound on completion time, or null for "All Time". */
    suspend fun getClassSummary(classId: String, since: String?): AppResult<ClassSummary> = safeCall {
        val quizIds = fetchLinkedQuizIds(classId)
        if (quizIds.isEmpty()) {
            return@safeCall ClassSummary(totalStudents = 0, totalQuizzes = 0, totalSubmissions = 0, averageScore = 0, averageTimeSeconds = null)
        }

        val completed = fetchResponses(quizIds, since, dateColumn = "completed_at", completedOnly = true)
        val avgScore = completed.mapNotNull { it.score }.average()
        val avgTime = completed.mapNotNull { secondsBetween(it.startedAt, it.completedAt) }.let { durations ->
            if (durations.isEmpty()) null else durations.average().roundToInt()
        }

        ClassSummary(
            totalStudents = completed.map { it.userEmail }.distinct().size,
            totalQuizzes = quizIds.size,
            totalSubmissions = completed.size,
            averageScore = if (avgScore.isNaN()) 0 else avgScore.roundToInt(),
            averageTimeSeconds = avgTime
        )
    }

    suspend fun getQuizPerformance(classId: String, since: String?): AppResult<List<ClassQuizPerformance>> = safeCall {
        val quizIds = fetchLinkedQuizIds(classId)
        if (quizIds.isEmpty()) return@safeCall emptyList()

        val quizzes = supabase.from("quizzes")
            .select(Columns.raw("id, title")) { filter { isIn("id", quizIds) } }
            .decodeList<QuizTitleRow>()

        // Every attempt (completed or not) is needed for the completion-rate denominator, scoped
        // by when it started rather than when/if it finished.
        val allAttempts = fetchResponses(quizIds, since, dateColumn = "started_at", completedOnly = false)
        val byQuiz = allAttempts.groupBy { it.quizId }

        quizzes.map { quiz ->
            val attempts = byQuiz[quiz.id].orEmpty()
            val completed = attempts.filter { it.completed == true }
            val avgScore = completed.mapNotNull { it.score }.average()
            val avgTime = completed.mapNotNull { secondsBetween(it.startedAt, it.completedAt) }.let { durations ->
                if (durations.isEmpty()) null else durations.average().roundToInt()
            }
            ClassQuizPerformance(
                quizId = quiz.id,
                quizTitle = quiz.title,
                participantCount = completed.map { it.userEmail }.distinct().size,
                submissionCount = completed.size,
                averageScore = if (avgScore.isNaN()) 0 else avgScore.roundToInt(),
                averageTimeSeconds = avgTime,
                completionRate = if (attempts.isEmpty()) 0 else (completed.size * 100 / attempts.size)
            )
        }
    }

    suspend fun getWeakLearners(classId: String, limit: Int = 50): AppResult<List<WeakLearner>> = safeCall {
        val quizIds = fetchLinkedQuizIds(classId)
        if (quizIds.isEmpty()) return@safeCall emptyList()

        val quizzes = supabase.from("quizzes")
            .select(Columns.raw("id, title")) { filter { isIn("id", quizIds) } }
            .decodeList<QuizTitleRow>()
        val titleById = quizzes.associate { it.id to it.title }

        supabase.from("quiz_responses")
            .select {
                filter { isIn("quiz_id", quizIds); eq("completed", true) }
                order("score", Order.ASCENDING)
                limit(limit.toLong())
            }
            .decodeList<QuizResponseDto>()
            .map { response ->
                WeakLearner(
                    studentName = response.userName?.takeIf { it.isNotBlank() } ?: response.userEmail,
                    studentEmail = response.userEmail,
                    quizTitle = titleById[response.quizId] ?: "Unknown quiz",
                    score = response.score ?: 0
                )
            }
    }

    /** Always all-time — see the KDoc on [ClassAiSummary] for why this isn't date-range scoped. */
    suspend fun getClassAiSummary(classId: String): AppResult<ClassAiSummary> = safeCall {
        supabase.postgrest.rpc(
            "get_class_ai_summary",
            buildJsonObject { put("p_class_id", classId) }
        ).decodeSingle<ClassAiSummaryDto>().toDomain()
    }

    private suspend fun fetchLinkedQuizIds(classId: String): List<String> =
        supabase.from("class_quizzes")
            .select(Columns.raw("quiz_id")) { filter { eq("class_id", classId) } }
            .decodeList<ClassQuizDto>()
            .mapNotNull { it.quizId }

    private suspend fun fetchResponses(
        quizIds: List<String>,
        since: String?,
        dateColumn: String,
        completedOnly: Boolean
    ): List<QuizResponseDto> = supabase.from("quiz_responses")
        .select {
            filter {
                isIn("quiz_id", quizIds)
                if (completedOnly) eq("completed", true)
                if (since != null) gte(dateColumn, since)
            }
        }
        .decodeList<QuizResponseDto>()

    private fun secondsBetween(startIso: String?, endIso: String?): Int? {
        if (startIso == null || endIso == null) return null
        return runCatching {
            (Instant.parse(endIso) - Instant.parse(startIso)).inWholeSeconds.toInt().coerceAtLeast(0)
        }.getOrNull()
    }
}
