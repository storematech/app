package com.quizmaker.android.repository

import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.network.safeCall
import com.quizmaker.android.data.model.Candidate
import com.quizmaker.android.data.model.VotingBallot
import com.quizmaker.android.data.model.VotingCampaign
import com.quizmaker.android.data.model.toDomain
import com.quizmaker.android.data.model.toDto
import com.quizmaker.android.data.remote.dto.VotingCampaignActiveUpdateDto
import com.quizmaker.android.data.remote.dto.VotingCampaignDto
import com.quizmaker.android.data.remote.dto.VotingCampaignInsertDto
import com.quizmaker.android.data.remote.dto.VotingCampaignUpdateDto
import com.quizmaker.android.data.remote.dto.VotingBallotDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Minimal row shape for the narrow `campaign_id`-only select in getVoteCounts(). */
@Serializable
private data class CampaignIdRow(@SerialName("campaign_id") val campaignId: String)

/**
 * Reads/writes `voting_campaigns`/`voting_ballots` — the fourth of the "Tools" (More → Tools),
 * teacher-facing campaign management only. Casting a ballot via the share link stays a web-only flow
 * (the DB already enforces one ballot per email via a partial unique index). Same shape as
 * PollRepository.
 */
@Singleton
class VotingRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getCampaigns(userId: String): AppResult<List<VotingCampaign>> = safeCall {
        supabase.from("voting_campaigns")
            .select {
                filter { eq("created_by", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<VotingCampaignDto>()
            .map { it.toDomain() }
    }

    suspend fun getCampaign(campaignId: String): AppResult<VotingCampaign> = safeCall {
        supabase.from("voting_campaigns")
            .select { filter { eq("id", campaignId) } }
            .decodeSingle<VotingCampaignDto>()
            .toDomain()
    }

    suspend fun getVoteCounts(campaignIds: List<String>): AppResult<Map<String, Int>> = safeCall {
        if (campaignIds.isEmpty()) return@safeCall emptyMap()
        supabase.from("voting_ballots")
            .select(Columns.raw("campaign_id")) { filter { isIn("campaign_id", campaignIds) } }
            .decodeList<CampaignIdRow>()
            .groupingBy { it.campaignId }
            .eachCount()
    }

    suspend fun createCampaign(
        userId: String,
        title: String,
        description: String?,
        candidates: List<Candidate>,
        requireEmail: Boolean,
        opensAt: String?,
        closesAt: String?,
        showResults: Boolean,
        isActive: Boolean
    ): AppResult<VotingCampaign> = safeCall {
        supabase.from("voting_campaigns")
            .insert(
                VotingCampaignInsertDto(
                    createdBy = userId,
                    title = title,
                    description = description,
                    slug = generateSlug(title),
                    candidates = candidates.map { it.toDto() },
                    requireEmail = requireEmail,
                    opensAt = opensAt,
                    closesAt = closesAt,
                    showResults = showResults,
                    isActive = isActive
                )
            ) { select() }
            .decodeSingle<VotingCampaignDto>()
            .toDomain()
    }

    suspend fun updateCampaign(
        campaignId: String,
        title: String,
        description: String?,
        candidates: List<Candidate>,
        requireEmail: Boolean,
        opensAt: String?,
        closesAt: String?,
        showResults: Boolean,
        isActive: Boolean
    ): AppResult<VotingCampaign> = safeCall {
        supabase.from("voting_campaigns")
            .update(
                VotingCampaignUpdateDto(
                    title = title,
                    description = description,
                    candidates = candidates.map { it.toDto() },
                    requireEmail = requireEmail,
                    opensAt = opensAt,
                    closesAt = closesAt,
                    showResults = showResults,
                    isActive = isActive
                )
            ) {
                filter { eq("id", campaignId) }
                select()
            }
            .decodeSingle<VotingCampaignDto>()
            .toDomain()
    }

    suspend fun setActive(campaignId: String, isActive: Boolean): AppResult<Unit> = safeCall {
        supabase.from("voting_campaigns").update(VotingCampaignActiveUpdateDto(isActive = isActive)) {
            filter { eq("id", campaignId) }
        }
        Unit
    }

    suspend fun deleteCampaign(campaignId: String): AppResult<Unit> = safeCall {
        supabase.from("voting_campaigns").delete { filter { eq("id", campaignId) } }
        Unit
    }

    suspend fun getBallots(campaignId: String): AppResult<List<VotingBallot>> = safeCall {
        supabase.from("voting_ballots")
            .select {
                filter { eq("campaign_id", campaignId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<VotingBallotDto>()
            .map { it.toDomain() }
    }

    /** `slug` is unique-constrained — a short random suffix avoids a pre-check round trip for the common case. */
    private fun generateSlug(title: String): String {
        val base = title.lowercase().trim()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "campaign" }
        return "$base-${UUID.randomUUID().toString().take(6)}"
    }
}
