package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.ReportedQuestion
import com.quizmaker.android.data.model.ReportedQuestionStatus
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.remote.dto.ReportedQuestionDto
import com.quizmaker.android.data.remote.dto.ReportedQuestionStatusUpdateDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads/updates `reported_questions` — the moderation queue of learner-flagged questions against
 * this creator's own quizzes. There's no write path for *creating* a report here since only
 * learners (the take-quiz flow, web-only in this app) file one.
 */
@Singleton
class ReportedQuestionsRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    // Explicit column list rather than "*": the table also carries `created_by` (used only to
    // scope the filter below, never decoded), and this app's SupabaseClient Json doesn't ignore
    // unknown keys — selecting "*" would throw a decode error against a DTO that omits it.
    private val reportColumns = Columns.raw(
        "id, question_id, question_text, quiz_id, quiz_title, reported_by, report_date, reason, status, response_id"
    )

    suspend fun getReportedQuestions(userId: String): AppResult<List<ReportedQuestion>> = safeCall {
        supabase.from("reported_questions")
            .select(reportColumns) {
                filter { eq("created_by", userId) }
                order("report_date", Order.DESCENDING)
            }
            .decodeList<ReportedQuestionDto>()
            .map { it.toDomain() }
    }

    /** Dashboard's "Reported" stat tile — pending reports only, so resolving one stops it counting. */
    suspend fun getPendingCount(userId: String): AppResult<Int> = safeCall {
        supabase.from("reported_questions")
            .select {
                filter {
                    eq("created_by", userId)
                    eq("status", ReportedQuestionStatus.PENDING.value)
                }
                count(Count.EXACT)
                head = true
            }
            .countOrNull()?.toInt() ?: 0
    }

    suspend fun updateStatus(reportId: String, status: ReportedQuestionStatus): AppResult<Unit> = safeCall {
        supabase.from("reported_questions")
            .update(ReportedQuestionStatusUpdateDto(status = status.value)) { filter { eq("id", reportId) } }
        Unit
    }
}
