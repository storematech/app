package com.quizmaker.android.data.model

import com.quizmaker.android.BuildConfig
import com.quizmaker.android.data.remote.dto.CandidateDto
import com.quizmaker.android.data.remote.dto.VotingBallotDto
import com.quizmaker.android.data.remote.dto.VotingCampaignDto

data class Candidate(
    val id: String,
    val name: String,
    val role: String? = null,
    val bio: String? = null
)

/** A shareable candidate-voting campaign — mirrors `VotingCampaign` in the web app's types/tools.ts. Each ballot picks exactly one candidate (unlike Poll, which can allow multiple). */
data class VotingCampaign(
    val id: String,
    val title: String,
    val description: String?,
    val slug: String,
    val candidates: List<Candidate>,
    val requireEmail: Boolean,
    val opensAt: String?,
    val closesAt: String?,
    val showResults: Boolean,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
) {
    val shareUrl: String get() = "${BuildConfig.SHARE_BASE_URL}/vote/$slug"
}

data class VotingBallot(
    val id: String,
    val campaignId: String,
    val candidateId: String,
    val voterName: String?,
    val voterEmail: String?,
    val createdAt: String
)

data class CandidateTally(val candidate: Candidate, val voteCount: Int, val percentage: Int)

fun CandidateDto.toDomain(): Candidate = Candidate(id = id, name = name, role = role, bio = bio)

fun Candidate.toDto(): CandidateDto = CandidateDto(id = id, name = name, role = role, bio = bio)

fun VotingCampaignDto.toDomain(): VotingCampaign = VotingCampaign(
    id = id,
    title = title,
    description = description,
    slug = slug,
    candidates = candidates.map { it.toDomain() },
    requireEmail = requireEmail,
    opensAt = opensAt,
    closesAt = closesAt,
    showResults = showResults,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun VotingBallotDto.toDomain(): VotingBallot = VotingBallot(
    id = id,
    campaignId = campaignId,
    candidateId = candidateId,
    voterName = voterName,
    voterEmail = voterEmail,
    createdAt = createdAt
)

/** Aggregates raw ballots into a per-candidate tally (count + rounded percentage), preserving the campaign's candidate order. */
fun VotingCampaign.tally(ballots: List<VotingBallot>): List<CandidateTally> {
    val counts = candidates.associate { candidate -> candidate.id to ballots.count { it.candidateId == candidate.id } }
    val total = counts.values.sum()
    return candidates.map { candidate ->
        val count = counts[candidate.id] ?: 0
        val percentage = if (total == 0) 0 else (count * 100) / total
        CandidateTally(candidate = candidate, voteCount = count, percentage = percentage)
    }
}
