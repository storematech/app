package com.quizmaker.android.ui.classdashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AiCardBg
import com.quizmaker.android.core.theme.AiCardBorder
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.StatAmberBg
import com.quizmaker.android.core.theme.StatAmberIcon
import com.quizmaker.android.core.theme.StatBlueBg
import com.quizmaker.android.core.theme.StatBlueIcon
import com.quizmaker.android.core.theme.StatGreenBg
import com.quizmaker.android.core.theme.StatGreenIcon
import com.quizmaker.android.core.theme.StatPurpleBg
import com.quizmaker.android.core.theme.StatPurpleIcon
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.ClassQuizPerformance
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.data.model.QuizAiSummary
import com.quizmaker.android.ui.common.AiSummaryCard
import com.quizmaker.android.ui.common.ClassAiSummaryCard
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.StatTile
import com.quizmaker.android.ui.common.elevatedSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDashboardScreen(
    onNavigateBack: () -> Unit,
    onOpenWeakLearners: (String) -> Unit,
    viewModel: ClassDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.quizClass?.name ?: "Class",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
            loadingContent = { ListScreenSkeleton(statTileCount = 3, rowCount = 4, rowLineWidths = listOf(200.dp, 150.dp)) }
        ) {
            when {
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                    ErrorBanner(message = uiState.errorMessage!!)
                }
                else -> {
                    val summary = uiState.summary
                    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
                        item {
                            RangeChipRow(selected = uiState.selectedRange, onSelect = viewModel::onRangeChange)
                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatTile(
                                    icon = Icons.Default.Group,
                                    iconBg = StatBlueBg,
                                    iconTint = StatBlueIcon,
                                    label = "Students",
                                    value = (summary?.totalStudents ?: 0).toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                StatTile(
                                    icon = Icons.AutoMirrored.Filled.Article,
                                    iconBg = StatPurpleBg,
                                    iconTint = StatPurpleIcon,
                                    label = "Quizzes",
                                    value = (summary?.totalQuizzes ?: 0).toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                StatTile(
                                    icon = Icons.Default.TrendingUp,
                                    iconBg = StatGreenBg,
                                    iconTint = StatGreenIcon,
                                    label = "Submissions",
                                    value = (summary?.totalSubmissions ?: 0).toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatTile(
                                    icon = Icons.Default.AutoAwesome,
                                    iconBg = StatAmberBg,
                                    iconTint = StatAmberIcon,
                                    label = "Avg Score",
                                    value = "${summary?.averageScore ?: 0}%",
                                    modifier = Modifier.weight(1f)
                                )
                                StatTile(
                                    icon = Icons.Default.Timer,
                                    iconBg = StatGreenBg,
                                    iconTint = StatGreenIcon,
                                    label = "Avg Time",
                                    value = summary?.averageTimeSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(Modifier.height(20.dp))
                            ClassAiSummaryCard(summary = uiState.classAiSummary, isLoading = uiState.isLoadingClassAiSummary)

                            Spacer(Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Quiz Performance", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                Text(
                                    "Add / Link Quizzes",
                                    color = BrandIndigo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    modifier = Modifier.clickable(onClick = viewModel::openLinkSheet)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        if (uiState.quizPerformance.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Default.GroupAdd,
                                    title = "No quizzes linked yet",
                                    subtitle = "Tap \"Add / Link Quizzes\" to bring existing quizzes into this class."
                                )
                            }
                        } else {
                            items(uiState.quizPerformance, key = { it.quizId }) { performance ->
                                ClassQuizPerformanceRow(
                                    performance = performance,
                                    aiSummary = uiState.aiSummaries[performance.quizId],
                                    isAiSummaryExpanded = performance.quizId in uiState.expandedAiSummaryQuizIds,
                                    isAiSummaryLoading = performance.quizId in uiState.loadingAiSummaryQuizIds,
                                    onToggleAiSummary = { viewModel.onToggleQuizAiSummary(performance.quizId) }
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }

                        item {
                            Spacer(Modifier.height(8.dp))
                            GradientButton(
                                text = "View Weak Learners",
                                onClick = { onOpenWeakLearners(uiState.quizClass?.id.orEmpty()) },
                                leadingIcon = Icons.Default.PriorityHigh,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }

    if (uiState.isLinkSheetOpen) {
        LinkQuizzesSheet(
            quizzes = uiState.linkableQuizzes,
            isLoading = uiState.isLoadingLinkableQuizzes,
            isLinking = uiState.isLinking,
            onDismiss = viewModel::closeLinkSheet,
            onLink = viewModel::linkQuiz
        )
    }
}

@Composable
private fun RangeChipRow(selected: ClassDateRangePreset, onSelect: (ClassDateRangePreset) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ClassDateRangePreset.entries.forEach { preset ->
            val isSelected = preset == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) BrandIndigo else SurfaceWhite)
                    .border(1.dp, if (isSelected) BrandIndigo else BorderGray, RoundedCornerShape(50))
                    .clickable { onSelect(preset) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    preset.label,
                    color = if (isSelected) Color.White else TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ClassQuizPerformanceRow(
    performance: ClassQuizPerformance,
    aiSummary: QuizAiSummary?,
    isAiSummaryExpanded: Boolean,
    isAiSummaryLoading: Boolean,
    onToggleAiSummary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                performance.quizTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            QuizAiToggleIcon(isExpanded = isAiSummaryExpanded, onClick = onToggleAiSummary)
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricLabel("Participants", performance.participantCount.toString())
            MetricLabel("Submissions", performance.submissionCount.toString())
            MetricLabel("Avg Score", "${performance.averageScore}%")
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetricLabel("Avg Time", performance.averageTimeSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-")
            MetricLabel("Completion Rate", "${performance.completionRate}%")
            Spacer(Modifier.weight(1f))
        }
        AnimatedVisibility(visible = isAiSummaryExpanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column {
                Spacer(Modifier.height(12.dp))
                AiSummaryCard(summary = aiSummary, isLoading = isAiSummaryLoading)
            }
        }
    }
}

@Composable
private fun MetricLabel(label: String, value: String) {
    Column {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun QuizAiToggleIcon(isExpanded: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(AiCardBg)
            .border(1.dp, AiCardBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.AutoAwesome,
            contentDescription = "AI Summary",
            tint = BrandIndigo,
            modifier = Modifier.size(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkQuizzesSheet(
    quizzes: List<Quiz>,
    isLoading: Boolean,
    isLinking: Boolean,
    onDismiss: () -> Unit,
    onLink: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = SurfaceWhite) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Add / Link Quizzes", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Pick an existing quiz to add to this class", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            when {
                isLoading -> Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandIndigo)
                }
                quizzes.isEmpty() -> Text(
                    "Every quiz you own is already linked to this class.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quizzes.forEach { quiz ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .elevatedSurface(shape = RoundedCornerShape(14.dp), elevation = 2.dp)
                                .clickable(enabled = !isLinking) { onLink(quiz.id) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(quiz.title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text("Link", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
