package com.quizmaker.android.ui.tools.feedback

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.PdfRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.StatGreenIcon
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.FeedbackSubmission
import com.quizmaker.android.ui.common.CsvFileIcon
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.GenericCsvExporter
import com.quizmaker.android.util.GenericTablePdfExporter
import com.quizmaker.android.util.formatDateTime
import com.quizmaker.android.util.formatShortDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSubmissionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: FeedbackSubmissionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.formTitle.isNotBlank()) "Submissions — ${uiState.formTitle}" else "Submissions",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    if (uiState.submissions.isNotEmpty()) {
                        IconButton(onClick = {
                            val intent = GenericTablePdfExporter.export(
                                context = context,
                                fileName = "feedback_submissions.pdf",
                                title = uiState.formTitle.ifBlank { "Submissions" },
                                columns = feedbackPdfColumns(uiState.fieldLabels),
                                rows = uiState.submissions.map { feedbackRow(it, uiState.fieldLabels) }
                            )
                            context.startActivity(Intent.createChooser(intent, "Export submissions (PDF)"))
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = PdfRed)
                        }
                        IconButton(onClick = {
                            val intent = GenericCsvExporter.export(
                                context = context,
                                fileName = "feedback_submissions.csv",
                                headers = feedbackHeaders(uiState.fieldLabels),
                                rows = uiState.submissions.map { feedbackRow(it, uiState.fieldLabels) }
                            )
                            context.startActivity(Intent.createChooser(intent, "Export submissions (CSV)"))
                        }) {
                            CsvFileIcon(contentDescription = "Export CSV")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LoadingCrossfade(
            isLoading = uiState.isLoading,
            modifier = Modifier.padding(padding),
            loadingContent = { ListScreenSkeleton(rowCount = 6, rowLineWidths = listOf(200.dp, 140.dp)) }
        ) {
            when {
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                    ErrorBanner(message = uiState.errorMessage!!)
                }
                uiState.submissions.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    EmptyState(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = "No submissions yet",
                        subtitle = "Once someone submits feedback, it shows up here."
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.submissions, key = { it.id }) { submission ->
                        SubmissionRow(submission = submission, fieldLabels = uiState.fieldLabels)
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SubmissionRow(submission: FeedbackSubmission, fieldLabels: Map<String, String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val headline = submission.learnerName?.takeIf { it.isNotBlank() } ?: submission.learnerEmail ?: "Anonymous"
            Text(
                headline,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            submission.overallRating?.let { rating ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = StatGreenIcon, modifier = Modifier.size(16.dp))
                    Text(" $rating/5", color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        if (!submission.learnerEmail.isNullOrBlank() && submission.learnerEmail != submission.learnerName) {
            Text(submission.learnerEmail, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (fieldLabels.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            fieldLabels.forEach { (fieldId, label) ->
                val value = submission.answers[fieldId]
                if (!value.isNullOrBlank()) {
                    Text("$label: $value", color = TextSecondary, fontSize = 12.5.sp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Submitted ${formatShortDate(submission.createdAt)}", color = TextSecondary, fontSize = 11.sp)
    }
}

private fun feedbackHeaders(fieldLabels: Map<String, String>): List<String> =
    listOf("Name", "Email", "Rating") + fieldLabels.values + "Submitted At"

private fun feedbackRow(submission: FeedbackSubmission, fieldLabels: Map<String, String>): List<String> =
    listOf(submission.learnerName.orEmpty(), submission.learnerEmail.orEmpty(), submission.overallRating?.toString().orEmpty()) +
        fieldLabels.keys.map { submission.answers[it].orEmpty() } +
        formatDateTime(submission.createdAt)

/** Fixed Name/Email/Rating/Submitted columns plus an even split of the remaining page width across whatever dynamic questions this form has. */
private fun feedbackPdfColumns(fieldLabels: Map<String, String>): List<Pair<String, Float>> {
    val dynamicWidth = if (fieldLabels.isEmpty()) 0f else (191f / fieldLabels.size).coerceAtLeast(40f)
    return listOf("Name" to 80f, "Email" to 110f, "Rating" to 50f) +
        fieldLabels.values.map { it to dynamicWidth } +
        listOf("Submitted" to 70f)
}
