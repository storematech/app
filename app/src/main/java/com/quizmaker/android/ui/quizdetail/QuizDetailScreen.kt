package com.quizmaker.android.ui.quizdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.ShareQuizSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDetailScreen(
    onNavigateBack: () -> Unit,
    onViewLeaderboard: (String) -> Unit,
    onEditQuiz: (String) -> Unit,
    viewModel: QuizDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onNavigateBack()
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text(uiState.quiz?.title ?: "Quiz details", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.quiz?.let { quiz ->
                        IconButton(onClick = { onEditQuiz(quiz.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showShareSheet = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                ErrorBanner(message = uiState.errorMessage!!)
            }
            uiState.quiz != null -> QuizDetailContent(
                quiz = uiState.quiz!!,
                questionCount = uiState.questionCount,
                responseCount = uiState.responseCount,
                actionInProgress = uiState.actionInProgress,
                modifier = Modifier.padding(padding),
                onViewLeaderboard = { onViewLeaderboard(uiState.quiz!!.id) },
                onCloseQuiz = viewModel::closeQuiz,
                onRequestDelete = { showDeleteConfirm = true }
            )
        }
    }

    if (showShareSheet && uiState.quiz != null) {
        ShareQuizSheet(quizTitle = uiState.quiz!!.title, shareUrl = uiState.quiz!!.shareUrl, onDismiss = { showShareSheet = false })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this quiz?") },
            text = { Text("This permanently deletes the quiz and all of its responses.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.deleteQuiz() }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun QuizDetailContent(
    quiz: Quiz,
    questionCount: Int,
    responseCount: Int,
    actionInProgress: Boolean,
    modifier: Modifier = Modifier,
    onViewLeaderboard: () -> Unit,
    onCloseQuiz: () -> Unit,
    onRequestDelete: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (quiz.description.isNotBlank()) {
            Text(quiz.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(label = "Questions", value = questionCount.toString(), modifier = Modifier.weight(1f))
            StatCard(label = "Responses", value = responseCount.toString(), modifier = Modifier.weight(1f))
            StatCard(label = "Status", value = if (quiz.isClosed) "Closed" else "Active", modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        Text("Settings", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingRow("Time limit", if (quiz.timeLimitType == "overall") "${quiz.timeLimit ?: 0} min overall" else "${quiz.timePerQuestion ?: 0}s per question")
                SettingRow("Shuffle questions", if (quiz.shuffleQuestions) "Yes" else "No")
                SettingRow("Leaderboard", if (quiz.showLeaderboard) "Enabled" else "Disabled")
                SettingRow("Multiple attempts", if (quiz.allowMultipleAttempts) "Allowed" else "Single attempt")
                SettingRow("OTP verification", if (quiz.requireOtpVerification) "Required" else "Not required")
            }
        }

        Spacer(Modifier.height(24.dp))
        if (quiz.showLeaderboard) {
            GradientButton(
                text = "View Leaderboard",
                onClick = onViewLeaderboard,
                leadingIcon = Icons.Default.EmojiEvents,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }
        if (!quiz.isClosed) {
            OutlinedButton(onClick = onCloseQuiz, enabled = !actionInProgress, modifier = Modifier.fillMaxWidth()) {
                Text("Close quiz")
            }
            Spacer(Modifier.height(12.dp))
        }
        TextButton(
            onClick = onRequestDelete,
            enabled = !actionInProgress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete quiz", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontFamily = PoppinsFamily, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
