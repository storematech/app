package com.quizmaker.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One entry of the `options` jsonb array on `polls`. */
@Serializable
data class PollOptionDto(
    val id: String,
    val label: String
)

/** Row shape of the `polls` table. */
@Serializable
data class PollDto(
    val id: String,
    @SerialName("created_by") val createdBy: String? = null,
    val question: String,
    val description: String? = null,
    val slug: String,
    val options: List<PollOptionDto> = emptyList(),
    @SerialName("allow_multiple") val allowMultiple: Boolean = false,
    @SerialName("show_results") val showResults: Boolean = true,
    @SerialName("closes_at") val closesAt: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class PollInsertDto(
    @SerialName("created_by") val createdBy: String,
    val question: String,
    val description: String?,
    val slug: String,
    val options: List<PollOptionDto>,
    @SerialName("allow_multiple") val allowMultiple: Boolean,
    @SerialName("show_results") val showResults: Boolean,
    @SerialName("closes_at") val closesAt: String?,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class PollUpdateDto(
    val question: String,
    val description: String?,
    val options: List<PollOptionDto>,
    @SerialName("allow_multiple") val allowMultiple: Boolean,
    @SerialName("show_results") val showResults: Boolean,
    @SerialName("closes_at") val closesAt: String?,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class PollActiveUpdateDto(
    @SerialName("is_active") val isActive: Boolean
)

/** Row shape of the `poll_votes` table — decode-only, mobile never writes votes. */
@Serializable
data class PollVoteDto(
    val id: String,
    @SerialName("poll_id") val pollId: String,
    @SerialName("option_ids") val optionIds: List<String> = emptyList(),
    @SerialName("voter_name") val voterName: String? = null,
    @SerialName("voter_email") val voterEmail: String? = null,
    @SerialName("created_at") val createdAt: String
)
