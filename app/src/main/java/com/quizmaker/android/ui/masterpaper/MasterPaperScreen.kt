package com.quizmaker.android.ui.masterpaper

import android.content.Intent
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.FilledPill
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.OutlinedPill
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.MasterPaperMode
import com.quizmaker.android.util.MasterPaperPdfExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterPaperScreen(
    onNavigateBack: () -> Unit,
    viewModel: MasterPaperViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun exportAndShare(mode: MasterPaperMode) {
        scope.launch {
            val branding = viewModel.getPdfBranding()
            val intent = MasterPaperPdfExporter.export(context, uiState.quizTitle, uiState.questions, mode, branding)
            context.startActivity(Intent.createChooser(intent, "Export Master Paper"))
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Master Paper", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (uiState.quizTitle.isNotBlank()) {
                            Text(uiState.quizTitle, fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LoadingCrossfade(
            isLoading = uiState.isLoading,
            modifier = Modifier.padding(padding),
            loadingContent = { ListScreenSkeleton(rowCount = 4, rowLineWidths = listOf(220.dp, 180.dp, 140.dp)) }
        ) {
            when {
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                    ErrorBanner(message = uiState.errorMessage!!)
                }
                uiState.questions.isEmpty() -> EmptyState(
                    icon = Icons.Default.Description,
                    title = "No questions in this quiz yet",
                    subtitle = "Add questions to generate a master paper."
                )
                else -> Column(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            FilledPill(text = "${uiState.questions.size} Questions")
                            Spacer(Modifier.height(4.dp))
                        }
                        itemsIndexed(uiState.questions, key = { _, q -> q.id }) { index, question ->
                            QuestionPreviewCard(index = index, question = question)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 16.dp, top = 4.dp)
                    ) {
                        GradientButton(
                            text = "Export with Answers",
                            onClick = { exportAndShare(MasterPaperMode.WITH_ANSWERS) },
                            leadingIcon = Icons.Default.Download,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { exportAndShare(MasterPaperMode.WITHOUT_ANSWERS) },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export without Answers")
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { exportAndShare(MasterPaperMode.OFFLINE) },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Offline Exam Paper")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionPreviewCard(index: Int, question: Question) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(18.dp), elevation = 3.dp)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledPill(text = "Q${index + 1}")
            Spacer(Modifier.width(8.dp))
            OutlinedPill(
                text = when (question.type) {
                    QuestionType.MULTI_CHOICE -> "Multiple Select"
                    QuestionType.FREE_TEXT -> "Free Text"
                    QuestionType.FILL_IN_BLANK -> "Fill in the Blank"
                    QuestionType.SINGLE_CHOICE -> "Single Choice"
                }
            )
            Spacer(Modifier.width(8.dp))
            Text("${question.points} pt${if (question.points > 1) "s" else ""}", color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Text(question.text, fontWeight = FontWeight.Medium, color = TextPrimary, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))

        when (question.type) {
            QuestionType.SINGLE_CHOICE, QuestionType.MULTI_CHOICE -> {
                Column {
                    question.options.forEachIndexed { optIndex, option ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Text(
                                "${('A' + optIndex)}. ",
                                fontWeight = FontWeight.Bold,
                                color = if (option.isCorrect) SuccessGreen else TextSecondary
                            )
                            Text(
                                option.text,
                                color = if (option.isCorrect) SuccessGreen else TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (option.isCorrect) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            else -> {
                Text("Answer: ${question.correctAnswer?.ifBlank { null } ?: "N/A"}", color = SuccessGreen, fontWeight = FontWeight.Medium)
            }
        }
    }
}
