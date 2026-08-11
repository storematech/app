package com.quizmaker.android.data.model

import com.quizmaker.android.BuildConfig
import com.quizmaker.android.data.remote.dto.PollDto
import com.quizmaker.android.data.remote.dto.PollOptionDto
import com.quizmaker.android.data.remote.dto.PollVoteDto

data class PollOption(val id: String, val label: String)

/** A shareable single-question poll — mirrors `Poll` in the web app's types/tools.ts. */
data class Poll(
    val id: String,
    val question: String,
    val description: String?,
    val slug: String,
    val options: List<PollOption>,
    val allowMultiple: Boolean,
    val showResults: Boolean,
    val closesAt: String?,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
) {
    val shareUrl: String get() = "${BuildConfig.SHARE_BASE_URL}/poll/$slug"
}

data class PollVote(
    val id: String,
    val pollId: String,
    val optionIds: List<String>,
    val voterName: String?,
    val voterEmail: String?,
    val createdAt: String
)

data class PollOptionTally(val option: PollOption, val voteCount: Int, val percentage: Int)

fun PollOptionDto.toDomain(): PollOption = PollOption(id = id, label = label)

fun PollOption.toDto(): PollOptionDto = PollOptionDto(id = id, label = label)

fun PollDto.toDomain(): Poll = Poll(
    id = id,
    question = question,
    description = description,
    slug = slug,
    options = options.map { it.toDomain() },
    allowMultiple = allowMultiple,
    showResults = showResults,
    closesAt = closesAt,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PollVoteDto.toDomain(): PollVote = PollVote(
    id = id,
    pollId = pollId,
    optionIds = optionIds,
    voterName = voterName,
    voterEmail = voterEmail,
    createdAt = createdAt
)

/** Aggregates raw votes into a per-option tally (count + rounded percentage), preserving the poll's option order. */
fun Poll.tally(votes: List<PollVote>): List<PollOptionTally> {
    val counts = options.associate { option -> option.id to votes.count { option.id in it.optionIds } }
    val total = counts.values.sum()
    return options.map { option ->
        val count = counts[option.id] ?: 0
        val percentage = if (total == 0) 0 else (count * 100) / total
        PollOptionTally(option = option, voteCount = count, percentage = percentage)
    }
}
