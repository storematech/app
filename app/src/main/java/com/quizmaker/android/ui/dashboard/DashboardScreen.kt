package com.quizmaker.android.ui.dashboard

import android.app.Activity
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoDark
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.StatAmberBg
import com.quizmaker.android.core.theme.StatAmberIcon
import com.quizmaker.android.core.theme.StatBlueBg
import com.quizmaker.android.core.theme.StatBlueIcon
import com.quizmaker.android.core.theme.StatGreenBg
import com.quizmaker.android.core.theme.StatGreenIcon
import com.quizmaker.android.core.theme.StatPurpleBg
import com.quizmaker.android.core.theme.StatPurpleIcon
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.QuizResponse
import com.quizmaker.android.ui.common.DesktopBanner
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.SaleDayBanner
import com.quizmaker.android.ui.common.SkeletonBox
import com.quizmaker.android.ui.common.SkeletonCardRow
import com.quizmaker.android.ui.common.SkeletonLine
import com.quizmaker.android.ui.common.SkeletonStatTiles
import com.quizmaker.android.ui.common.StatTile
import com.quizmaker.android.ui.common.elevatedSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenResponse: (String) -> Unit,
    onOpenQuizzes: () -> Unit,
    onOpenQuestions: () -> Unit,
    onOpenResponses: () -> Unit,
    onOpenPricing: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // The banner is the first item in the scrollable list (so it scrolls away like normal
    // content, not pinned) but bleeds edge-to-edge behind the status bar for that first,
    // unscrolled frame — so status bar icons need to be light/white only while it's still the
    // first visible item, flipping to dark once it's scrolled past a plain/light background.
    val listState = rememberLazyListState()
    val bannerVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    val view = LocalView.current
    val insetsController = remember(view) {
        (view.context as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view) }
    }
    DisposableEffect(insetsController) {
        val wasLight = insetsController?.isAppearanceLightStatusBars
        onDispose { if (wasLight != null) insetsController?.isAppearanceLightStatusBars = wasLight }
    }
    SideEffect { insetsController?.isAppearanceLightStatusBars = !bannerVisible }

    // The bottom nav bar's own Scaffold (NavGraph) already reserves the system nav-bar inset;
    // reserving it again here would leave a redundant empty strip above the tab bar.
    Scaffold(containerColor = AppBackground, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        LoadingCrossfade(
            isLoading = uiState.isLoading,
            modifier = Modifier.padding(padding),
            loadingContent = { DashboardSkeleton() }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // No horizontal/top inset here — the banner item needs to reach the true screen
                // edges and the top of the screen. Every other item pads itself horizontally instead.
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                item {
                    val activeSale = uiState.activeSale
                    if (activeSale != null) {
                        SaleDayBanner(saleName = activeSale.name, onClick = onOpenPricing)
                    } else {
                        WelcomeBanner()
                    }
                    Spacer(Modifier.height(16.dp))
                }

                uiState.errorMessage?.let { message ->
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            ErrorBanner(message = message)
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                item {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceWhite, RoundedCornerShape(50))
                                    .border(1.dp, BorderGray, RoundedCornerShape(50))
                                    .clickable { menuExpanded = true }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(uiState.selectedRange.label, color = TextPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextSecondary)
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DashboardDateRange.entries.forEach { range ->
                                    DropdownMenuItem(
                                        text = { Text(range.label) },
                                        onClick = { menuExpanded = false; viewModel.onRangeSelected(range) }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            StatTile(
                                icon = Icons.Default.NoteAdd,
                                iconBg = StatBlueBg,
                                iconTint = StatBlueIcon,
                                label = "Total Quizzes",
                                value = uiState.totalQuizzes.toString(),
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable(onClick = onOpenQuizzes)
                            )
                            StatTile(
                                icon = Icons.Default.Assessment,
                                iconBg = StatPurpleBg,
                                iconTint = StatPurpleIcon,
                                label = "Question Bank",
                                value = uiState.totalQuestions.toString(),
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable(onClick = onOpenQuestions)
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            StatTile(
                                icon = Icons.Default.CheckCircle,
                                iconBg = StatGreenBg,
                                iconTint = StatGreenIcon,
                                label = "Completions",
                                value = uiState.totalResponses.toString(),
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable(onClick = onOpenResponses)
                            )
                            StatTile(
                                icon = Icons.Default.Assessment,
                                iconBg = StatAmberBg,
                                iconTint = StatAmberIcon,
                                label = "Avg Score",
                                value = "${uiState.averageScorePercent}%",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        DesktopBanner()
                        Spacer(Modifier.height(24.dp))
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text("Recent Submissions", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = { Text("Search by name or email...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }

                if (uiState.recentSubmissions.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.CheckCircle,
                            title = if (uiState.searchQuery.isBlank()) "No submissions yet" else "No matches",
                            subtitle = if (uiState.searchQuery.isBlank()) "Completed quiz attempts will show up here." else "Try a different name or email.",
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                } else {
                    items(uiState.recentSubmissions, key = { it.id }) { submission ->
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            RecentSubmissionRow(
                                submission = submission,
                                quizTitle = uiState.quizTitleById[submission.quizId],
                                onClick = { onOpenResponse(submission.id) }
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSubmissionRow(
    submission: QuizResponse,
    quizTitle: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(submission.userName?.ifBlank { submission.userEmail } ?: submission.userEmail, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(submission.userEmail, color = TextSecondary, fontSize = 13.sp)
                if (!quizTitle.isNullOrBlank()) {
                    Text(quizTitle, color = TextSecondary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SuccessGreen)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Completed", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Time: ${submission.timeTakenSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-"}",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Text("${submission.score} pts", fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
private fun WelcomeBanner() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(BrandIndigoDark, BrandIndigo)),
                RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 26.dp)
    ) {
        Text("Welcome to Yuno LMS", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text("Everything you need to run great assessments", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            WelcomeFeature(icon = Icons.Default.NoteAdd, label = "Create Quiz", modifier = Modifier.weight(1f))
            WelcomeFeature(icon = Icons.Default.Share, label = "Share Quiz", modifier = Modifier.weight(1f))
            WelcomeFeature(icon = Icons.Default.Assessment, label = "Advanced Reports", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun WelcomeFeature(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun DashboardSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        // Rough stand-in for the welcome/sale banner — full-bleed and behind the status bar,
        // same as the real thing, so there's no layout jump once it loads in.
        SkeletonBox(
            modifier = Modifier.fillMaxWidth().height(190.dp),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
        )
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(50.dp))
            Spacer(Modifier.height(20.dp))
            SkeletonStatTiles(count = 2)
            Spacer(Modifier.height(14.dp))
            SkeletonStatTiles(count = 2)
            Spacer(Modifier.height(24.dp))
            SkeletonLine(width = 160.dp, height = 18.dp)
            Spacer(Modifier.height(10.dp))
            SkeletonBox(modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp))
            Spacer(Modifier.height(12.dp))
            repeat(3) {
                SkeletonCardRow(lineWidths = listOf(140.dp, 180.dp, 100.dp))
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}
