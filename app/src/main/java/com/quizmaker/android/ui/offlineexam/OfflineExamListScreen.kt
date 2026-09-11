package com.quizmaker.android.ui.offlineexam

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.ui.common.BlurBehindDialog
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.formatShortDate

/**
 * "Offline Exams" list — real `quizzes` rows flagged `is_offline_exam = true` (see
 * OfflineExamListViewModel/QuizRepository.getOfflineExams), listed separately from the main Quiz
 * List. Each row's Download button opens the same live paper preview CreateOfflineExamScreen's
 * "Preview Paper" button does (see OfflineExamPaperPreviewScreen) rather than exporting straight
 * from here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineExamListScreen(
    onNavigateBack: () -> Unit,
    onCreateNew: () -> Unit,
    onOpenExam: (String) -> Unit,
    onOpenPaperPreview: (String) -> Unit,
    viewModel: OfflineExamListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var quizPendingDelete by remember { mutableStateOf<Quiz?>(null) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Offline Exams", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNew, containerColor = BrandIndigo) {
                Icon(Icons.Default.Add, contentDescription = "New Offline Exam", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->
        LoadingCrossfade(
            isLoading = uiState.isLoading,
            modifier = Modifier.padding(padding),
            loadingContent = { ListScreenSkeleton(rowCount = 4, rowLineWidths = listOf(200.dp, 160.dp, 120.dp)) }
        ) {
            Column(Modifier.fillMaxSize()) {
                uiState.errorMessage?.let {
                    Box(Modifier.fillMaxWidth().padding(20.dp)) {
                        ErrorBanner(message = it, onRetry = viewModel::refresh)
                    }
                }
                if (uiState.exams.isEmpty() && uiState.errorMessage == null) {
                    EmptyState(
                        icon = Icons.Default.Description,
                        title = "No offline exams yet",
                        subtitle = "Create one to get started."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.exams, key = { it.id }) { quiz ->
                            OfflineExamRow(
                                quiz = quiz,
                                questionCount = uiState.questionCounts[quiz.id],
                                isDeleting = quiz.id == uiState.deletingQuizId,
                                onClick = { onOpenExam(quiz.id) },
                                onDownload = { onOpenPaperPreview(quiz.id) },
                                onRequestDelete = { quizPendingDelete = quiz }
                            )
                        }
                        item { Spacer(Modifier.size(72.dp)) }
                    }
                }
            }
        }
    }

    quizPendingDelete?.let { quiz ->
        AlertDialog(
            onDismissRequest = { quizPendingDelete = null },
            title = { BlurBehindDialog(); Text("Delete \"${quiz.title}\"?") },
            text = { Text("This permanently deletes this offline exam and its saved questions link — the questions themselves stay in your question bank.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExam(quiz.id)
                    quizPendingDelete = null
                }) { Text("Delete", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { quizPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun OfflineExamRow(
    quiz: Quiz,
    questionCount: Int?,
    isDeleting: Boolean,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onRequestDelete: () -> Unit
) {
    // "Download" opens OfflineExamPaperPreviewScreen synchronously — no in-place loading state
    // needed here (unlike the old direct-export flow this replaced).
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(20.dp))
            .clickable(enabled = !isDeleting, onClick = onClick)
            .padding(18.dp)
            .then(if (isDeleting) Modifier.alpha(0.5f) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(quiz.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(formatShortDate(quiz.createdAt), color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.width(12.dp))
                Text(if (isDeleting) "Deleting…" else "${questionCount ?: "-"} questions", color = TextSecondary, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(BrandIndigo.copy(alpha = if (isDeleting) 0.5f else 1f), CircleShape)
        ) {
            IconButton(onClick = onDownload, enabled = !isDeleting, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.Download, contentDescription = "Download", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
        Box {
            IconButton(onClick = { menuExpanded = true }, enabled = !isDeleting) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options", tint = TextSecondary)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Delete", color = ErrorRed) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                    onClick = {
                        menuExpanded = false
                        onRequestDelete()
                    }
                )
            }
        }
    }
}
