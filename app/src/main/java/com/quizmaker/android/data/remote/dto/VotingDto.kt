package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One entry of the `candidates` jsonb array on `voting_campaigns`. */
@Serializable
data class CandidateDto(
    val id: String,
    val name: String,
    val role: String? = null,
    val bio: String? = null
)

/** Row shape of the `voting_campaigns` table. */
@Serializable
data class VotingCampaignDto(
    val id: String,
    @SerialName("created_by") val createdBy: String? = null,
    val title: String,
    val description: String? = null,
    val slug: String,
    val candidates: List<CandidateDto> = emptyList(),
    @SerialName("require_email") val requireEmail: Boolean = true,
    @SerialName("opens_at") val opensAt: String? = null,
    @SerialName("closes_at") val closesAt: String? = null,
    @SerialName("show_results") val showResults: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class VotingCampaignInsertDto(
    @SerialName("created_by") val createdBy: String,
    val title: String,
    val description: String?,
    val slug: String,
    val candidates: List<CandidateDto>,
    @SerialName("require_email") val requireEmail: Boolean,
    @SerialName("opens_at") val opensAt: String?,
    @SerialName("closes_at") val closesAt: String?,
    @SerialName("show_results") val showResults: Boolean,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class VotingCampaignUpdateDto(
    val title: String,
    val description: String?,
    val candidates: List<CandidateDto>,
    @SerialName("require_email") val requireEmail: Boolean,
    @SerialName("opens_at") val opensAt: String?,
    @SerialName("closes_at") val closesAt: String?,
    @SerialName("show_results") val showResults: Boolean,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class VotingCampaignActiveUpdateDto(
    @SerialName("is_active") val isActive: Boolean
)

/** Row shape of the `voting_ballots` table — decode-only, mobile never writes ballots. */
@Serializable
data class VotingBallotDto(
    val id: String,
    @SerialName("campaign_id") val campaignId: String,
    @SerialName("candidate_id") val candidateId: String,
    @SerialName("voter_name") val voterName: String? = null,
    @SerialName("voter_email") val voterEmail: String? = null,
    @SerialName("created_at") val createdAt: String
)
