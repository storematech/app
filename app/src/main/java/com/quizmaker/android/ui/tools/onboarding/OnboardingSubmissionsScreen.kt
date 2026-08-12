package com.quizmaker.android.ui.tools.onboarding

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.OnboardingSubmission
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSubmissionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: OnboardingSubmissionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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
                            scope.launch {
                                val branding = viewModel.getPdfBranding()
                                val intent = GenericTablePdfExporter.export(
                                    context = context,
                                    fileName = "onboarding_submissions.pdf",
                                    title = uiState.formTitle.ifBlank { "Submissions" },
                                    columns = onboardingPdfColumns(uiState.fieldLabels),
                                    rows = uiState.submissions.map { onboardingRow(it, uiState.fieldLabels) },
                                    branding = branding
                                )
                                context.startActivity(Intent.createChooser(intent, "Export submissions (PDF)"))
                            }
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = PdfRed)
                        }
                        IconButton(onClick = {
                            val intent = GenericCsvExporter.export(
                                context = context,
                                fileName = "onboarding_submissions.csv",
                                headers = onboardingHeaders(uiState.fieldLabels),
                                rows = uiState.submissions.map { onboardingRow(it, uiState.fieldLabels) }
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
                        icon = Icons.Default.Group,
                        title = "No submissions yet",
                        subtitle = "Once someone fills out this form, their details show up here."
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
private fun SubmissionRow(submission: OnboardingSubmission, fieldLabels: Map<String, String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
            .padding(14.dp)
    ) {
        val headline = submission.name?.takeIf { it.isNotBlank() } ?: submission.email ?: "Anonymous"
        Text(headline, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (!submission.email.isNullOrBlank() && submission.email != headline) {
            Text(submission.email, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!submission.phone.isNullOrBlank()) {
            Text(submission.phone, color = TextSecondary, fontSize = 12.sp)
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

private fun onboardingHeaders(fieldLabels: Map<String, String>): List<String> =
    listOf("Name", "Email", "Phone") + fieldLabels.values + "Submitted At"

private fun onboardingRow(submission: OnboardingSubmission, fieldLabels: Map<String, String>): List<String> =
    listOf(submission.name.orEmpty(), submission.email.orEmpty(), submission.phone.orEmpty()) +
        fieldLabels.keys.map { submission.answers[it].orEmpty() } +
        formatDateTime(submission.createdAt)

/** Fixed Name/Email/Phone/Submitted columns plus an even split of the remaining page width across whatever dynamic fields this form has. */
private fun onboardingPdfColumns(fieldLabels: Map<String, String>): List<Pair<String, Float>> {
    val dynamicWidth = if (fieldLabels.isEmpty()) 0f else (201f / fieldLabels.size).coerceAtLeast(40f)
    return listOf("Name" to 80f, "Email" to 110f, "Phone" to 70f) +
        fieldLabels.values.map { it to dynamicWidth } +
        listOf("Submitted" to 70f)
}
