package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.QuizAiSummary
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.remote.dto.QuizAiSummaryDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the cached per-quiz stat rollup shown to users as "AI Summary" — it's plain SQL
 * aggregation (see get_quiz_ai_summary in supabase/sql/quiz_ai_summary.sql), not real AI. The
 * Postgres function owns caching/staleness itself, so this is always a single cheap RPC call.
 */
@Singleton
class QuizAiSummaryRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getSummary(quizId: String, notifyOnError: Boolean = true): AppResult<QuizAiSummary> = safeCall(notifyOnError) {
        supabase.postgrest.rpc(
            "get_quiz_ai_summary",
            buildJsonObject { put("p_quiz_id", quizId) }
        ).decodeSingle<QuizAiSummaryDto>().toDomain()
    }
}
