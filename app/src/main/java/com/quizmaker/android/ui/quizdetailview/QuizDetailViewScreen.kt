package com.quizmaker.android.ui.quizdetailview

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PdfRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.StatAmberBg
import com.quizmaker.android.core.theme.StatAmberIcon
import com.quizmaker.android.core.theme.StatBlueBg
import com.quizmaker.android.core.theme.StatBlueIcon
import com.quizmaker.android.core.theme.StatGreenBg
import com.quizmaker.android.core.theme.StatGreenIcon
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.QuizDetailViewData
import com.quizmaker.android.data.model.StudentAnswerRow
import com.quizmaker.android.ui.common.AiSummaryCard
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.CsvFileIcon
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.OutlinedPill
import com.quizmaker.android.ui.common.ScorePill
import com.quizmaker.android.ui.common.StatTile
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.QuizDetailCsvExporter
import com.quizmaker.android.util.QuizDetailPdfExporter
import com.quizmaker.android.util.formatDateTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailViewScreen(
    onNavigateBack: () -> Unit,
    onOpenResponse: (String) -> Unit,
    viewModel: QuizDetailViewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var isExporting by remember { mutableStateOf(false) }
    var isAiSummaryExpanded by remember { mutableStateOf(true) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisible >= totalItems - 4
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    fun exportWith(action: (QuizDetailViewData) -> Unit) {
        if (isExporting) return
        scope.launch {
            isExporting = true
            viewModel.loadAllForExport()?.let(action)
            isExporting = false
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quiz Detail View", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        uiState.summary?.quizTitle?.let { Text(it, fontSize = 12.sp, color = TextSecondary) }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isExporting) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BrandIndigo, strokeWidth = 2.dp)
                        }
                    } else {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        if ((uiState.summary?.totalResponses ?: 0) > 0) {
                            IconButton(onClick = {
                                exportWith { data ->
                                    val intent = QuizDetailCsvExporter.export(context, data)
                                    context.startActivity(Intent.createChooser(intent, "Export detail view (CSV)"))
                                }
                            }) {
                                CsvFileIcon(contentDescription = "Export CSV")
                            }
                            IconButton(onClick = {
                                exportWith { data ->
                                    val intent = QuizDetailPdfExporter.export(context, data)
                                    context.startActivity(Intent.createChooser(intent, "Export detail view (PDF)"))
                                }
                            }) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = PdfRed)
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        LoadingCrossfade(
            isLoading = uiState.isLoading,
            modifier = Modifier.padding(padding),
            loadingContent = { ListScreenSkeleton(statTileCount = 3, rowCount = 5, rowLineWidths = listOf(150.dp, 200.dp, 120.dp)) }
        ) {
            when {
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                    ErrorBanner(message = uiState.errorMessage!!)
                }
                uiState.summary != null -> {
                    val summary = uiState.summary!!
                    LazyColumn(state = listState, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatTile(
                                    icon = Icons.Default.Group,
                                    iconBg = StatBlueBg,
                                    iconTint = StatBlueIcon,
                                    label = "Responses",
                                    value = summary.totalResponses.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                StatTile(
                                    icon = Icons.Default.Assessment,
                                    iconBg = StatAmberBg,
                                    iconTint = StatAmberIcon,
                                    label = "Avg Score",
                                    value = "${summary.averageScore}%",
                                    modifier = Modifier.weight(1f)
                                )
                                StatTile(
                                    icon = Icons.Default.Timer,
                                    iconBg = StatGreenBg,
                                    iconTint = StatGreenIcon,
                                    label = "Avg Time",
                                    value = summary.averageTimeSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            AiSummaryCard(
                                summary = uiState.aiSummary,
                                isLoading = uiState.isLoadingAiSummary,
                                isExpanded = isAiSummaryExpanded,
                                onToggleExpanded = { isAiSummaryExpanded = !isAiSummaryExpanded }
                            )
                            Spacer(Modifier.height(20.dp))
                        }

                        if (uiState.rows.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Default.Description,
                                    title = "No completed attempts yet",
                                    subtitle = "Once people take this quiz, their results show up here."
                                )
                            }
                        } else {
                            items(uiState.rows, key = { it.responseId }) { row ->
                                StudentRowCard(
                                    row = row,
                                    gradedCount = summary.gradedCount,
                                    showAttempt = summary.allowMultipleAttempts,
                                    onClick = { onOpenResponse(row.responseId) }
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                            if (uiState.isLoadingMore) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BrandIndigo)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentRowCard(row: StudentAnswerRow, gradedCount: Int, showAttempt: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(row.email, color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.width(8.dp))
            ScorePill(percent = row.scorePercent)
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Correct: ${row.correctCount}/$gradedCount", color = TextSecondary, fontSize = 12.sp)
            Text("Time: ${row.timeSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-"}", color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatDateTime(row.completedAt), color = TextSecondary, fontSize = 12.sp)
            if (showAttempt) {
                OutlinedPill(text = "Attempt ${row.attemptNumber}")
            }
        }
    }
}
