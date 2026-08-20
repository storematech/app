package com.quizmaker.android.ui.learners

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PdfRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.Learner
import com.quizmaker.android.data.model.LearnerQuizAttempt
import com.quizmaker.android.ui.common.CsvFileIcon
import com.quizmaker.android.ui.common.OutlinedPill
import com.quizmaker.android.ui.common.ScorePill
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.formatShortDate
import kotlin.math.roundToInt

/** Read-only learner detail view — name/roster info plus summary stats and their recent quiz attempts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProfileDialog(
    learner: Learner,
    attempts: List<LearnerQuizAttempt>,
    isLoadingAttempts: Boolean,
    onExportRequest: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = SurfaceWhite) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(learner.name, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                    Text("ID: ${learner.studentId}", color = TextSecondary, fontSize = 13.sp)
                }
                IconButton(onClick = { onExportRequest(ExportFormat.PDF) }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = PdfRed)
                }
                IconButton(onClick = { onExportRequest(ExportFormat.CSV) }) {
                    CsvFileIcon(contentDescription = "Export CSV")
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth().elevatedSurface(shape = RoundedCornerShape(16.dp), color = AppBackground, elevation = 0.dp).padding(16.dp)
            ) {
                DetailRow(label = "Email", value = learner.email?.takeIf { it.isNotBlank() } ?: "—")
                learner.groupName?.let {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Group", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        OutlinedPill(text = it, borderColor = BrandIndigo, contentColor = BrandIndigo)
                    }
                }
                if (!learner.parentName.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    DetailRow(label = "Parent Name", value = learner.parentName)
                }
                if (!learner.parentContact.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    DetailRow(label = "Parent Contact", value = learner.parentContact)
                }
                learner.createdAt?.let {
                    Spacer(Modifier.height(10.dp))
                    DetailRow(label = "Added", value = formatShortDate(it))
                }
            }

            if (!learner.notes.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("NOTES", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(6.dp))
                Text(learner.notes, color = TextPrimary, fontSize = 14.sp)
            }

            if (attempts.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("SUMMARY", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(10.dp))
                val avgScore = attempts.mapNotNull { it.score }.average().takeIf { !it.isNaN() }?.roundToInt() ?: 0
                val avgTimeSeconds = attempts.mapNotNull { it.timeTaken }.average().takeIf { !it.isNaN() }?.roundToInt()
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatChip(label = "Quizzes", value = attempts.size.toString(), modifier = Modifier.weight(1f))
                    StatChip(label = "Avg Score", value = "$avgScore%", modifier = Modifier.weight(1f))
                    StatChip(
                        label = "Avg Time",
                        value = avgTimeSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("RECENT QUIZ ATTEMPTS", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(10.dp))

            when {
                isLoadingAttempts -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(20.dp), color = BrandIndigo)
                    }
                }
                attempts.isEmpty() -> {
                    Text("No quiz attempts yet.", color = TextSecondary, fontSize = 13.sp)
                }
                else -> {
                    attempts.forEach { attempt ->
                        AttemptRow(attempt)
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .elevatedSurface(shape = RoundedCornerShape(14.dp), color = AppBackground, elevation = 0.dp)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AttemptRow(attempt: LearnerQuizAttempt) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(14.dp), elevation = 2.dp)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(attempt.quizTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
            attempt.completedAt?.let {
                Spacer(Modifier.height(2.dp))
                Text(formatShortDate(it), color = TextSecondary, fontSize = 12.sp)
            }
        }
        attempt.score?.let { ScorePill(percent = it) }
    }
}
