package com.quizmaker.android.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * A starter recipe for [VotingEditSheet] — picking one prefills the create sheet, same idea as
 * [defaultCandidates] but for the campaign framing. Candidate names are always genuinely
 * election-specific, so unlike Onboarding/Feedback/Poll templates this only prefills
 * title/description — candidates stay the standard blank pair the sheet already starts with.
 */
data class VotingTemplate(
    val icon: ImageVector,
    val label: String,
    val summary: String,
    val title: String,
    val description: String
)

val VOTING_TEMPLATES: List<VotingTemplate> = listOf(
    VotingTemplate(
        icon = Icons.Default.HowToVote,
        label = "Class Representative",
        summary = "Elect a class representative",
        title = "Class Representative Election",
        description = "Vote for your class representative."
    ),
    VotingTemplate(
        icon = Icons.Default.Groups,
        label = "Student Council President",
        summary = "Elect the student council president",
        title = "Student Council President Election",
        description = "Cast your vote for student council president."
    ),
    VotingTemplate(
        icon = Icons.Default.WorkspacePremium,
        label = "Best Teacher Award",
        summary = "Recognize an outstanding teacher",
        title = "Best Teacher Award",
        description = "Vote for the teacher who inspired you most this year."
    ),
    VotingTemplate(
        icon = Icons.Default.Shield,
        label = "House Captain Election",
        summary = "Elect a house captain",
        title = "House Captain Election",
        description = "Vote for your house captain."
    ),
    VotingTemplate(
        icon = Icons.Default.SportsScore,
        label = "Sports Captain Election",
        summary = "Elect a sports team captain",
        title = "Sports Captain Election",
        description = "Vote for your sports captain."
    ),
    VotingTemplate(
        icon = Icons.Default.Star,
        label = "Best Project Award",
        summary = "Recognize the best student project",
        title = "Best Project Award",
        description = "Vote for the project that impressed you most."
    ),
    VotingTemplate(
        icon = Icons.Default.MilitaryTech,
        label = "Annual Day Theme",
        summary = "Vote on the theme for Annual Day",
        title = "Annual Day Theme Vote",
        description = "Vote for your favorite Annual Day theme."
    ),
    VotingTemplate(
        icon = Icons.Default.School,
        label = "Club President Election",
        summary = "Elect a club president",
        title = "Club President Election",
        description = "Vote for your club's next president."
    ),
    VotingTemplate(
        icon = Icons.Default.MusicNote,
        label = "Best Performance Award",
        summary = "Recognize a standout talent show act",
        title = "Best Performance Award",
        description = "Vote for the performance you enjoyed most."
    ),
    VotingTemplate(
        icon = Icons.Default.EmojiEvents,
        label = "Alumni of the Year",
        summary = "Honor an outstanding alumnus",
        title = "Alumni of the Year",
        description = "Vote for the alumnus who made us proudest this year."
    )
)
