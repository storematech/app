package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.RevisionItem
import com.quizmaker.android.data.model.RevisionStatus
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.remote.dto.RevisionItemInsertDto
import com.quizmaker.android.data.remote.dto.RevisionItemJoinDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** Minimal row shape for the existence check in [RevisionRepository.addForRevision]. */
@Serializable
private data class RevisionIdRow(val id: String)

/** Reads/writes the `revision_items` table backing the "Revision" (bookmarked questions) feature. */
@Singleton
class RevisionRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getRevisionItems(userId: String): AppResult<List<RevisionItem>> = safeCall {
        supabase.from("revision_items")
            .select(Columns.raw("id, quiz_id, status, created_at, questions(*, options(*)), quizzes(title)")) {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<RevisionItemJoinDto>()
            .map { it.toDomain() }
    }

    /** No-ops if [questionId] is already bookmarked for [userId] — `unique(user_id, question_id)` backs this, but
     *  checking first avoids the caller ever seeing a constraint-violation error for what's really just a no-op. */
    suspend fun addForRevision(userId: String, questionId: String, quizId: String?): AppResult<Unit> = safeCall {
        val existing = supabase.from("revision_items")
            .select(Columns.raw("id")) {
                filter { eq("user_id", userId); eq("question_id", questionId) }
            }
            .decodeList<RevisionIdRow>()
        if (existing.isEmpty()) {
            supabase.from("revision_items").insert(RevisionItemInsertDto(userId = userId, questionId = questionId, quizId = quizId))
        }
        Unit
    }

    suspend fun removeFromRevision(userId: String, questionId: String): AppResult<Unit> = safeCall {
        supabase.from("revision_items").delete {
            filter { eq("user_id", userId); eq("question_id", questionId) }
        }
        Unit
    }

    suspend fun deleteItems(ids: List<String>): AppResult<Unit> = safeCall {
        if (ids.isNotEmpty()) {
            supabase.from("revision_items").delete { filter { isIn("id", ids) } }
        }
        Unit
    }

    /** Backs both "Mark as Done" and its undo (revert to pending) — same shape either direction. */
    suspend fun updateStatus(ids: List<String>, status: RevisionStatus): AppResult<Unit> = safeCall {
        if (ids.isNotEmpty()) {
            val statusValue = if (status == RevisionStatus.DONE) "done" else "pending"
            supabase.from("revision_items").update({ set("status", statusValue) }) {
                filter { isIn("id", ids) }
            }
        }
        Unit
    }
}
