package com.quizmaker.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.ClassAiSummary

/**
 * "Class AI Summary" — same visual language and the same rule as [AiSummaryCard]: this is a cached
 * SQL aggregate (get_class_ai_summary, see supabase/sql/classes.sql) across every quiz in a class,
 * branded to read as a live insight via [TypewriterText]. No AI/ML involved anywhere.
 */
@Composable
fun ClassAiSummaryCard(
    summary: ClassAiSummary?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    errorMessage: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AiCardBg)
            .border(1.dp, AiCardBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Class AI Summary", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = PoppinsFamily)
        }
        Spacer(Modifier.height(12.dp))
        when {
            isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = BrandIndigo, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Generating insights…", color = TextSecondary, fontSize = 13.sp)
            }
            errorMessage != null -> Text(errorMessage, color = TextSecondary, fontSize = 13.sp)
            summary == null || summary.participantCount == 0 -> Text(
                "Not enough submissions yet — link a quiz with responses to see class-wide insights.",
                color = TextSecondary,
                fontSize = 13.sp
            )
            else -> TypewriterText(text = buildClassSummaryText(summary), color = TextPrimary)
        }
    }
}

private class ClassSummaryValues(summary: ClassAiSummary) {
    val quizCount = summary.quizCount
    val participantCount = summary.participantCount
    val avg = summary.averageScore
    val bestWorstClause: String = when {
        summary.bestQuizTitle != null && summary.worstQuizTitle != null && summary.bestQuizTitle != summary.worstQuizTitle ->
            " Students performed best in ${summary.bestQuizTitle} and struggled most with ${summary.worstQuizTitle}."
        summary.bestQuizTitle != null ->
            " Performance has been consistent across quizzes so far."
        else -> ""
    }
}

private val classTemplates: List<(ClassSummaryValues) -> String> = listOf(
    { v -> "This class is averaging ${v.avg}% across ${v.quizCount} quizzes and ${v.participantCount} submissions.${v.bestWorstClause}" },
    { v -> "${v.quizCount} quizzes, ${v.participantCount} submissions, ${v.avg}% average so far.${v.bestWorstClause}" },
    { v -> "Across ${v.quizCount} quizzes in this class, students are averaging ${v.avg}%.${v.bestWorstClause}" },
    { v -> "This class has ${v.participantCount} submissions across ${v.quizCount} quizzes, averaging ${v.avg}%.${v.bestWorstClause}" }
)

private fun buildClassSummaryText(summary: ClassAiSummary): String {
    val values = ClassSummaryValues(summary)
    val hash = summary.classId.hashCode()
    val index = ((hash % classTemplates.size) + classTemplates.size) % classTemplates.size
    return classTemplates[index](values)
}
