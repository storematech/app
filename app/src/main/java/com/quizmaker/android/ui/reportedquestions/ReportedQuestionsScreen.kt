package com.quizmaker.android.ui.reportedquestions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.StatRedIcon
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.ReportedQuestion
import com.quizmaker.android.data.model.ReportedQuestionStatus
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportedQuestionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportedQuestionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Reported Questions", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusFilterPill(selected = uiState.statusFilter, onSelect = viewModel::onStatusFilterSelected)
            }

            LoadingCrossfade(
                isLoading = uiState.isLoading,
                loadingContent = { ListScreenSkeleton(rowCount = 6, rowLineWidths = listOf(150.dp, 180.dp, 120.dp)) }
            ) {
                when {
                    uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                        ErrorBanner(message = uiState.errorMessage!!, onRetry = viewModel::refresh)
                    }
                    uiState.filteredReports.isEmpty() -> EmptyState(
                        icon = Icons.Default.Flag,
                        title = "No reported questions",
                        subtitle = "Questions learners flag while taking a quiz will show up here."
                    )
                    else -> LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
                        items(uiState.filteredReports, key = { it.id }) { report ->
                            ReportedQuestionCard(
                                report = report,
                                isUpdating = report.id in uiState.updatingIds,
                                onToggleStatus = { viewModel.toggleStatus(report) }
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                        item { Spacer(Modifier.height(60.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusFilterPill(selected: ReportedQuestionStatusFilter, onSelect: (ReportedQuestionStatusFilter) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SurfaceWhite)
                .border(1.dp, BorderGray, RoundedCornerShape(50))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selected.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReportedQuestionStatusFilter.entries.forEach { filter ->
                DropdownMenuItem(text = { Text(filter.label) }, onClick = { expanded = false; onSelect(filter) })
            }
        }
    }
}

@Composable
private fun ReportedQuestionCard(report: ReportedQuestion, isUpdating: Boolean, onToggleStatus: () -> Unit) {
    val isPending = report.status == ReportedQuestionStatus.PENDING
    val statusColor = if (isPending) StatRedIcon else SuccessGreen
    val statusLabel = if (isPending) "Pending" else "Resolved"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(report.quizTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(report.questionText, color = TextSecondary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(statusColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(statusLabel, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Reason: ${report.reason}", color = TextPrimary, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text("Reported by: ${report.reportedBy}", color = TextSecondary, fontSize = 12.sp)
        Text("Reported: ${formatDateTime(report.reportDate)}", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onToggleStatus, enabled = !isUpdating) {
            if (isUpdating) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text(if (isPending) "Mark Resolved" else "Reopen")
            }
        }
    }
}
