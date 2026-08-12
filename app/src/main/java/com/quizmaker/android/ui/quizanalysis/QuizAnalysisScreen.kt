package com.quizmaker.android.ui.quizanalysis

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
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
import com.quizmaker.android.data.model.QuestionStat
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.StatTile
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.ui.common.scoreBandColor
import com.quizmaker.android.util.QuizAnalysisPdfExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizAnalysisScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuizAnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Question Performance", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        uiState.data?.quizTitle?.let { Text(it, fontSize = 12.sp, color = TextSecondary) }
                    }
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
                    val data = uiState.data
                    if (data != null && data.questions.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch {
                                val branding = viewModel.getPdfBranding()
                                val intent = QuizAnalysisPdfExporter.export(context, data, branding)
                                context.startActivity(Intent.createChooser(intent, "Export question performance (PDF)"))
                            }
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = PdfRed)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LoadingCrossfade(
            isLoading = uiState.isLoading,
            modifier = Modifier.padding(padding),
            loadingContent = { ListScreenSkeleton(statTileCount = 3, rowCount = 5, rowLineWidths = listOf(220.dp, 180.dp)) }
        ) {
            when {
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                    ErrorBanner(message = uiState.errorMessage!!)
                }
                uiState.data != null -> {
                    val data = uiState.data!!
                    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
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
                                    value = data.totalResponses.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                StatTile(
                                    icon = Icons.Default.Assessment,
                                    iconBg = StatAmberBg,
                                    iconTint = StatAmberIcon,
                                    label = "Avg Score",
                                    value = "${data.averageScore}%",
                                    modifier = Modifier.weight(1f)
                                )
                                StatTile(
                                    icon = Icons.Default.Timer,
                                    iconBg = StatGreenBg,
                                    iconTint = StatGreenIcon,
                                    label = "Avg Time",
                                    value = data.averageTimeSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "Question breakdown",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        if (data.questions.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Default.BarChart,
                                    title = "No questions in this quiz yet",
                                    subtitle = "Analysis appears once the quiz has questions and responses."
                                )
                            }
                        } else if (data.totalResponses == 0) {
                            item {
                                EmptyState(
                                    icon = Icons.Default.BarChart,
                                    title = "No completed attempts yet",
                                    subtitle = "Once people take this quiz, per-question analysis shows up here."
                                )
                            }
                        } else {
                            items(data.questions, key = { it.questionId }) { stat ->
                                QuestionStatRow(
                                    stat = stat,
                                    isBookmarked = stat.questionId in uiState.bookmarkedQuestionIds,
                                    onToggleRevision = { viewModel.toggleRevision(stat.questionId) }
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionStatRow(stat: QuestionStat, isBookmarked: Boolean, onToggleRevision: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Q${stat.order}. ${stat.text}",
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            if (!stat.isUngraded) {
                Text("${stat.correctPercent}%", color = scoreBandColor(stat.correctPercent), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            IconButton(onClick = onToggleRevision, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Remove from revision" else "Add for revision",
                    tint = BrandIndigo
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        if (stat.isUngraded) {
            Text("Ungraded — ${stat.attempted} attempted, ${stat.skippedCount} skipped", color = TextSecondary, fontSize = 12.sp)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(BorderGray)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(stat.correctPercent / 100f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(scoreBandColor(stat.correctPercent))
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${stat.correctCount} correct · ${stat.wrongCount} wrong · ${stat.skippedCount} skipped",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
