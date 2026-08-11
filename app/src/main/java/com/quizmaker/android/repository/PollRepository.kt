package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.Poll
import com.quizmaker.android.data.model.PollOption
import com.quizmaker.android.data.model.PollVote
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.model.toDto
import com.quizmaker.android.data.remote.dto.PollActiveUpdateDto
import com.quizmaker.android.data.remote.dto.PollDto
import com.quizmaker.android.data.remote.dto.PollInsertDto
import com.quizmaker.android.data.remote.dto.PollUpdateDto
import com.quizmaker.android.data.remote.dto.PollVoteDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Minimal row shape for the narrow `poll_id`-only select in getVoteCounts(). */
@Serializable
private data class PollIdRow(@SerialName("poll_id") val pollId: String)

/**
 * Reads/writes `polls`/`poll_votes` — the third of the "Tools" (More → Tools), teacher-facing poll
 * management only. Voting via the share link stays a web-only flow. Same shape as
 * OnboardingFormRepository/FeedbackFormRepository.
 */
@Singleton
class PollRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getPolls(userId: String): AppResult<List<Poll>> = safeCall {
        supabase.from("polls")
            .select {
                filter { eq("created_by", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<PollDto>()
            .map { it.toDomain() }
    }

    suspend fun getPoll(pollId: String): AppResult<Poll> = safeCall {
        supabase.from("polls")
            .select { filter { eq("id", pollId) } }
            .decodeSingle<PollDto>()
            .toDomain()
    }

    suspend fun getVoteCounts(pollIds: List<String>): AppResult<Map<String, Int>> = safeCall {
        if (pollIds.isEmpty()) return@safeCall emptyMap()
        supabase.from("poll_votes")
            .select(Columns.raw("poll_id")) { filter { isIn("poll_id", pollIds) } }
            .decodeList<PollIdRow>()
            .groupingBy { it.pollId }
            .eachCount()
    }

    suspend fun createPoll(
        userId: String,
        question: String,
        description: String?,
        options: List<PollOption>,
        allowMultiple: Boolean,
        showResults: Boolean,
        closesAt: String?,
        isActive: Boolean
    ): AppResult<Poll> = safeCall {
        supabase.from("polls")
            .insert(
                PollInsertDto(
                    createdBy = userId,
                    question = question,
                    description = description,
                    slug = generateSlug(question),
                    options = options.map { it.toDto() },
                    allowMultiple = allowMultiple,
                    showResults = showResults,
                    closesAt = closesAt,
                    isActive = isActive
                )
            ) { select() }
            .decodeSingle<PollDto>()
            .toDomain()
    }

    suspend fun updatePoll(
        pollId: String,
        question: String,
        description: String?,
        options: List<PollOption>,
        allowMultiple: Boolean,
        showResults: Boolean,
        closesAt: String?,
        isActive: Boolean
    ): AppResult<Poll> = safeCall {
        supabase.from("polls")
            .update(
                PollUpdateDto(
                    question = question,
                    description = description,
                    options = options.map { it.toDto() },
                    allowMultiple = allowMultiple,
                    showResults = showResults,
                    closesAt = closesAt,
                    isActive = isActive
                )
            ) {
                filter { eq("id", pollId) }
                select()
            }
            .decodeSingle<PollDto>()
            .toDomain()
    }

    suspend fun setActive(pollId: String, isActive: Boolean): AppResult<Unit> = safeCall {
        supabase.from("polls").update(PollActiveUpdateDto(isActive = isActive)) {
            filter { eq("id", pollId) }
        }
        Unit
    }

    suspend fun deletePoll(pollId: String): AppResult<Unit> = safeCall {
        supabase.from("polls").delete { filter { eq("id", pollId) } }
        Unit
    }

    suspend fun getVotes(pollId: String): AppResult<List<PollVote>> = safeCall {
        supabase.from("poll_votes")
            .select {
                filter { eq("poll_id", pollId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<PollVoteDto>()
            .map { it.toDomain() }
    }

    /** `slug` is unique-constrained — a short random suffix avoids a pre-check round trip for the common case. */
    private fun generateSlug(question: String): String {
        val base = question.lowercase().trim()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "poll" }
        return "$base-${UUID.randomUUID().toString().take(6)}"
    }
}
