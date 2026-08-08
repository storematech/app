package com.quizmaker.android.ui.responses

import android.content.Intent
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PdfRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.core.theme.WarningAmber
import com.quizmaker.android.data.model.QuizResponse
import com.quizmaker.android.ui.common.CsvFileIcon
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.ui.common.scoreBandColor
import com.quizmaker.android.util.ResponsesCsvExporter
import com.quizmaker.android.util.ResponsesPdfExporter
import com.quizmaker.android.util.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsesScreen(
    onNavigateBack: () -> Unit,
    onOpenResponse: (String) -> Unit,
    viewModel: ResponsesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Responses", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    if (uiState.filteredResponses.isNotEmpty()) {
                        IconButton(onClick = {
                            val intent = ResponsesPdfExporter.export(context, uiState.filteredResponses, uiState.quizTitleById)
                            context.startActivity(Intent.createChooser(intent, "Export responses (PDF)"))
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = PdfRed)
                        }
                        IconButton(onClick = {
                            val intent = ResponsesCsvExporter.export(context, uiState.filteredResponses, uiState.quizTitleById)
                            context.startActivity(Intent.createChooser(intent, "Export responses (CSV)"))
                        }) {
                            CsvFileIcon(contentDescription = "Export CSV")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = uiState.searchEmail,
                    onValueChange = viewModel::onSearchEmailChange,
                    placeholder = { Text("Search by name or email...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuizFilterPill(
                        selectedQuizId = uiState.selectedQuizId,
                        quizzes = uiState.quizzes,
                        onSelect = viewModel::onQuizSelected
                    )
                    StatusFilterPill(selected = uiState.statusFilter, onSelect = viewModel::onStatusSelected)
                    TimeFrameFilterPill(selected = uiState.timeFrame, onSelect = viewModel::onTimeFrameSelected)
                }
            }

            LoadingCrossfade(
                isLoading = uiState.isLoading,
                loadingContent = {
                    ListScreenSkeleton(rowCount = 6, rowLineWidths = listOf(150.dp, 180.dp, 120.dp))
                }
            ) {
                when {
                    uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                        ErrorBanner(message = uiState.errorMessage!!)
                    }
                    uiState.filteredResponses.isEmpty() -> EmptyState(
                        icon = Icons.Default.Description,
                        title = "No responses found",
                        subtitle = "Try adjusting your filters."
                    )
                    else -> LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)) {
                        items(uiState.filteredResponses, key = { it.id }) { response ->
                            ResponseCard(
                                response = response,
                                quizTitle = uiState.quizTitleById[response.quizId] ?: "Unknown Quiz",
                                onClick = { if (response.completed) onOpenResponse(response.id) }
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
private fun FilterPill(label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(SurfaceWhite)
                .border(1.dp, BorderGray, RoundedCornerShape(50))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
        content()
    }
}

@Composable
private fun QuizFilterPill(selectedQuizId: String, quizzes: List<com.quizmaker.android.data.model.Quiz>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (selectedQuizId == "all") "All Quizzes" else quizzes.firstOrNull { it.id == selectedQuizId }?.title ?: "All Quizzes"
    FilterPill(label = label, onClick = { expanded = true }) {
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All Quizzes") }, onClick = { expanded = false; onSelect("all") })
            quizzes.forEach { quiz ->
                DropdownMenuItem(text = { Text(quiz.title) }, onClick = { expanded = false; onSelect(quiz.id) })
            }
        }
    }
}

@Composable
private fun StatusFilterPill(selected: ResponseStatusFilter, onSelect: (ResponseStatusFilter) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    FilterPill(label = selected.label, onClick = { expanded = true }) {
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ResponseStatusFilter.entries.forEach { status ->
                DropdownMenuItem(text = { Text(status.label) }, onClick = { expanded = false; onSelect(status) })
            }
        }
    }
}

@Composable
private fun TimeFrameFilterPill(selected: ResponseTimeFrame, onSelect: (ResponseTimeFrame) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    FilterPill(label = selected.label, onClick = { expanded = true }) {
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ResponseTimeFrame.entries.forEach { timeFrame ->
                DropdownMenuItem(text = { Text(timeFrame.label) }, onClick = { expanded = false; onSelect(timeFrame) })
            }
        }
    }
}

@Composable
private fun ResponseCard(response: QuizResponse, quizTitle: String, onClick: () -> Unit) {
    val status = if (response.cancelled) "Cancelled" else if (response.completed) "Completed" else "In Progress"
    val statusColor = if (response.cancelled) ErrorRed else if (response.completed) SuccessGreen else WarningAmber

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
            .clickable(enabled = response.completed, onClick = onClick)
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(quizTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    response.userName?.ifBlank { null } ?: response.userEmail,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(statusColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(status, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Score: ${response.score}%", color = scoreBandColor(response.score), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                "Time: ${response.timeTakenSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-"}",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("Started: ${formatDateTime(response.startedAt)}", color = TextSecondary, fontSize = 12.sp)
    }
}
