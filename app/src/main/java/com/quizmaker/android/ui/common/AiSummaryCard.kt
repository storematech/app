package com.quizmaker.android.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.AiCardBg
import com.quizmaker.android.core.theme.AiCardBorder
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.ScoreHighBg
import com.quizmaker.android.core.theme.ScoreLowBg
import com.quizmaker.android.core.theme.ScoreMidBg
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.LearnerConfidence
import com.quizmaker.android.data.model.QuizAiSummary
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * "AI Summary" card shared by the Quiz List (per-card, lazy) and Quiz Detail View (eager) screens.
 * Branded to read as a live AI-generated insight — one of 15 fixed sentence/bullet templates
 * (picked deterministically per quiz, see [templateIndexFor], so the same quiz always reads the
 * same way but different quizzes don't all sound identical) with numbers substituted in, revealed
 * with a typewriter animation. Every value actually comes straight from get_quiz_ai_summary, a
 * plain SQL aggregate — see [QuizAiSummary]'s KDoc: no AI/ML involved anywhere.
 *
 * [isExpanded]/[onToggleExpanded] are optional — pass both to get a collapse chevron in the
 * header (used by Quiz Detail View, where the card is always present on the page). Quiz List
 * leaves them at their defaults since the whole card is already shown/hidden by its own AI icon.
 */
@Composable
fun AiSummaryCard(
    summary: QuizAiSummary?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    isExpanded: Boolean = true,
    onToggleExpanded: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AiCardBg)
            .border(1.dp, AiCardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onToggleExpanded != null) Modifier.clickable(onClick = onToggleExpanded) else Modifier),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("AI Summary", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = PoppinsFamily)
            }
            if (onToggleExpanded != null) {
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = BrandIndigo
                )
            }
        }

        AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column {
                Spacer(Modifier.height(12.dp))
                when {
                    isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = BrandIndigo, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Generating insights…", color = TextSecondary, fontSize = 13.sp)
                    }
                    errorMessage != null -> Text(errorMessage, color = TextSecondary, fontSize = 13.sp)
                    summary == null || summary.participantCount == 0 -> Text(
                        "Not enough responses yet — check back once people take this quiz.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    else -> {
                        TypewriterSummary(summary = summary)
                        Spacer(Modifier.height(12.dp))
                        val badgeColor = when (summary.confidence) {
                            LearnerConfidence.POSITIVE -> ScoreHighBg
                            LearnerConfidence.NEUTRAL -> ScoreMidBg
                            LearnerConfidence.NEGATIVE -> ScoreLowBg
                        }
                        val badgeLabel = when (summary.confidence) {
                            LearnerConfidence.POSITIVE -> "Learner Confidence: Positive"
                            LearnerConfidence.NEUTRAL -> "Learner Confidence: Neutral"
                            LearnerConfidence.NEGATIVE -> "Learner Confidence: Negative"
                        }
                        // Same confidence colors as everywhere else in the app — just lightened
                        // (container at low alpha, text at full alpha) to match this card's softer look.
                        FilledPill(text = badgeLabel, containerColor = badgeColor.copy(alpha = 0.16f), contentColor = badgeColor)
                    }
                }
            }
        }
    }
}

/** Picks one of the 15 templates and types it out; replays whenever the underlying quiz/values change. */
@Composable
private fun TypewriterSummary(summary: QuizAiSummary) {
    val content = remember(summary) { buildSummaryContent(summary) }
    val bodyColor = TextPrimary

    when (content) {
        is SummaryContent.Paragraph -> TypewriterText(text = content.text, color = bodyColor)
        is SummaryContent.Bullets -> {
            val joined = remember(content) { content.lines.joinToString("\n") { "•  $it" } }
            TypewriterText(text = joined, color = bodyColor)
        }
    }
}

/**
 * Character-by-character reveal, like text streaming in live — plays once per [text], then stays
 * fully revealed. `rememberSaveable` (not plain `remember`) is the point: this card lives inside a
 * LazyColumn item and/or a navigable screen, both of which dispose and rebuild this composable
 * fresh — on a plain scroll-off-and-back, or a navigate-away-and-back (e.g. tapping into a
 * participant's response and returning) — which would otherwise replay the typing animation every
 * single time instead of just the first.
 */
@Composable
internal fun TypewriterText(text: String, color: androidx.compose.ui.graphics.Color) {
    var visibleChars by rememberSaveable(text) { mutableStateOf(0) }

    LaunchedEffect(text) {
        if (visibleChars >= text.length) return@LaunchedEffect // already fully typed once — show as-is, don't replay
        // Always finishes in ~1.8s (90 ticks * 20ms) regardless of sentence length, by revealing
        // more characters per tick for longer text instead of a fixed per-character delay.
        val increment = (text.length / 90).coerceAtLeast(1)
        while (visibleChars < text.length) {
            visibleChars = (visibleChars + increment).coerceAtMost(text.length)
            delay(20)
        }
    }

    val cursor = if (visibleChars < text.length) "▍" else ""
    Text(text = text.take(visibleChars) + cursor, color = color, fontSize = 14.sp, lineHeight = 21.sp)
}

private sealed class SummaryContent {
    data class Paragraph(val text: String) : SummaryContent()
    data class Bullets(val lines: List<String>) : SummaryContent()
}

/** Values pre-formatted for interpolation, shared across every template below. */
private class SummaryValues(summary: QuizAiSummary) {
    val count = summary.participantCount.toString()
    val avg = summary.averageScore
    val median = summary.medianScore
    // Spelled out rather than a bare "(median X%)" — most people don't parse "median" at a
    // glance, but "most participants scored near X%" reads clearly without any explanation needed.
    val medianPhrase = "most participants scored near ${median}%"
    val correct = formatCount(summary.avgCorrectCount)
    val wrong = formatCount(summary.avgWrongCount)
    val time: String? = summary.avgTimeSeconds?.let { "${it / 60}m ${it % 60}s" }
    val timeSuffix = time?.let { ", finishing in about $it" }.orEmpty()
    val confidenceLong = when (summary.confidence) {
        LearnerConfidence.POSITIVE -> "strong — most learners are scoring well"
        LearnerConfidence.NEUTRAL -> "moderate — scores are fairly mixed"
        LearnerConfidence.NEGATIVE -> "low — most learners are finding this quiz difficult"
    }
    val confidenceShort = when (summary.confidence) {
        LearnerConfidence.POSITIVE -> "strong"
        LearnerConfidence.NEUTRAL -> "moderate"
        LearnerConfidence.NEGATIVE -> "low"
    }
}

private fun formatCount(value: Double): String = String.format(Locale.US, "%.1f", value)

/** 10 paragraph phrasings + 5 bullet phrasings — picked deterministically by quiz id below. */
private val paragraphTemplates: List<(SummaryValues) -> String> = listOf(
    { v -> "${v.count} people have taken this quiz so far, averaging ${v.avg}% — ${v.medianPhrase}. On a typical attempt, learners get ${v.correct} questions right and ${v.wrong} wrong${v.timeSuffix}. Overall learner confidence looks ${v.confidenceLong}." },
    { v -> "${v.count} learners have attempted this quiz, scoring ${v.avg}% on average. The typical participant answers ${v.correct} questions correctly and misses ${v.wrong}${v.timeSuffix}. Confidence trend: ${v.confidenceShort}." },
    { v -> "So far ${v.count} people have taken this quiz. Average score sits at ${v.avg}%, and ${v.medianPhrase}. Most attempts get ${v.correct} right and ${v.wrong} wrong${v.timeSuffix}." },
    { v -> "${v.count} attempts recorded. Scores average ${v.avg}%, and ${v.medianPhrase}. Learners typically get ${v.correct} correct, ${v.wrong} incorrect${v.timeSuffix}. This points to ${v.confidenceLong}." },
    { v -> "Out of ${v.count} attempts, the average score is ${v.avg}%. Learners usually answer ${v.correct} questions correctly and ${v.wrong} incorrectly${v.timeSuffix} — confidence here looks ${v.confidenceShort}." },
    { v -> "${v.count} participants, ${v.avg}% average score. Typical performance: ${v.correct} correct answers, ${v.wrong} wrong${v.timeSuffix}. Learner confidence: ${v.confidenceShort}." },
    { v -> "This quiz has been taken ${v.count} times, averaging ${v.avg}% — ${v.medianPhrase}. Participants get roughly ${v.correct} right and ${v.wrong} wrong on a typical run${v.timeSuffix}, suggesting confidence is ${v.confidenceShort}." },
    { v -> "Across ${v.count} attempts, the average score is ${v.avg}%, and ${v.medianPhrase}. Learners answer about ${v.correct} questions correctly and ${v.wrong} incorrectly${v.timeSuffix}. Confidence looks ${v.confidenceShort} overall." },
    { v -> "${v.count} people have completed this quiz with an average score of ${v.avg}%. A typical attempt sees ${v.correct} correct answers and ${v.wrong} mistakes${v.timeSuffix} — confidence trend is ${v.confidenceShort}." },
    { v -> "With ${v.count} attempts so far, scores average ${v.avg}%, and ${v.medianPhrase}. Most learners get ${v.correct} questions right and ${v.wrong} wrong${v.timeSuffix}, pointing to ${v.confidenceShort} learner confidence." }
)

private val bulletTemplates: List<(SummaryValues) -> List<String>> = listOf(
    { v ->
        listOfNotNull(
            "${v.count} people have taken this quiz",
            "Average score: ${v.avg}%",
            "Most participants scored near ${v.median}%",
            "Typically ${v.correct} correct, ${v.wrong} wrong",
            v.time?.let { "Average time: $it" },
            "Learner confidence: ${v.confidenceShort}"
        )
    },
    { v ->
        listOfNotNull(
            "Attempts: ${v.count}",
            "Average score: ${v.avg}% (most scored near ${v.median}%)",
            "Right vs wrong per attempt: ${v.correct} vs ${v.wrong}",
            v.time?.let { "Typical time: $it" },
            "Confidence: ${v.confidenceShort}"
        )
    },
    { v ->
        listOfNotNull(
            "${v.count} learners attempted this quiz",
            "Scores average ${v.avg}%",
            "Most get ${v.correct} questions right, ${v.wrong} wrong",
            "Overall confidence looks ${v.confidenceShort}"
        )
    },
    { v ->
        listOfNotNull(
            "Total attempts: ${v.count}",
            "Average score: ${v.avg}%",
            "Most participants scored near ${v.median}%",
            "Correct vs wrong: ${v.correct} vs ${v.wrong}",
            v.time?.let { "Average time: $it" },
            "Confidence: ${v.confidenceShort}"
        )
    },
    { v ->
        listOfNotNull(
            "${v.count} people have taken this quiz so far",
            "They're averaging ${v.avg}% correct",
            "A typical run: ${v.correct} right, ${v.wrong} wrong",
            v.time?.let { "in about $it" },
            "Confidence trend: ${v.confidenceShort}"
        )
    }
)

private val allTemplateCount = paragraphTemplates.size + bulletTemplates.size

/** Deterministic per-quiz pick — same quiz always reads the same way, different quizzes vary. */
private fun templateIndexFor(quizId: String): Int {
    val hash = quizId.hashCode()
    return ((hash % allTemplateCount) + allTemplateCount) % allTemplateCount
}

private fun buildSummaryContent(summary: QuizAiSummary): SummaryContent {
    val values = SummaryValues(summary)
    val index = templateIndexFor(summary.quizId)
    return if (index < paragraphTemplates.size) {
        SummaryContent.Paragraph(paragraphTemplates[index](values))
    } else {
        SummaryContent.Bullets(bulletTemplates[index - paragraphTemplates.size](values))
    }
}
