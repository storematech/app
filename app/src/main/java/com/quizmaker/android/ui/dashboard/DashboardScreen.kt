package com.quizmaker.android.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoDark
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.StatAmberBg
import com.quizmaker.android.core.theme.StatAmberIcon
import com.quizmaker.android.core.theme.StatBlueBg
import com.quizmaker.android.core.theme.StatBlueIcon
import com.quizmaker.android.core.theme.StatGreenBg
import com.quizmaker.android.core.theme.StatGreenIcon
import com.quizmaker.android.core.theme.StatPurpleBg
import com.quizmaker.android.core.theme.StatPurpleIcon
import com.quizmaker.android.core.theme.StatRedBg
import com.quizmaker.android.core.theme.StatRedIcon
import com.quizmaker.android.core.theme.StatRoseBg
import com.quizmaker.android.core.theme.StatRoseIcon
import com.quizmaker.android.core.theme.StatTealBg
import com.quizmaker.android.core.theme.StatTealIcon
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.data.model.QuizResponse
import com.quizmaker.android.ui.common.DesktopBanner
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.FeatureTourBanner
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.SaleDayBanner
import com.quizmaker.android.ui.common.SkeletonBox
import com.quizmaker.android.ui.common.SkeletonCardRow
import com.quizmaker.android.ui.common.SkeletonLine
import com.quizmaker.android.ui.common.SkeletonStatTiles
import com.quizmaker.android.ui.common.StatTile
import com.quizmaker.android.ui.common.TrialActiveBanner
import com.quizmaker.android.ui.common.TrialEndedBanner
import com.quizmaker.android.ui.common.TrialExtendedBanner
import com.quizmaker.android.ui.common.TrialPaywallSheet
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.TrialStatus
import com.quizmaker.android.util.formatShortDate
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenResponse: (String) -> Unit,
    onOpenQuizzes: () -> Unit,
    onOpenQuiz: (String) -> Unit,
    onCreateQuiz: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenClasses: () -> Unit,
    onOpenQuestions: () -> Unit,
    onOpenResponses: () -> Unit,
    onOpenReportedQuestions: () -> Unit,
    onOpenLearners: () -> Unit,
    onOpenPricing: () -> Unit,
    onOpenFeatureTour: () -> Unit,
    onOpenOmrScan: (String) -> Unit,
    onOpenOfflineExamList: () -> Unit,
    onOpenCertificateDesigner: () -> Unit,
    onOpenImportQuestions: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Scanner quick action: gates on the same CAMERA runtime permission it always has (the actual
    // photo capture now happens inside OmrScanScreen itself, not here) -- a granted permission opens
    // a lightweight "which quiz is this for" picker, then hands off to the OMR scan/review flow for
    // that quiz. See OmrScanScreen/OmrScanViewModel.
    val context = LocalContext.current
    var showOmrQuizPicker by remember { mutableStateOf(false) }

    val scanCameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showOmrQuizPicker = true
    }

    fun onScannerTapped() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            showOmrQuizPicker = true
        } else {
            scanCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (showOmrQuizPicker) {
        OmrQuizPickerDialog(
            quizzes = uiState.quizzes,
            onDismiss = { showOmrQuizPicker = false },
            onQuizSelected = { quizId ->
                showOmrQuizPicker = false
                onOpenOmrScan(quizId)
            }
        )
    }

    // Fires every time this screen (re)enters composition — including navigating back to the
    // Dashboard tab after creating a quiz/question/learner elsewhere — but only actually re-fetches
    // if DashboardStateCache.needsRefresh was flagged dirty by that action; see refreshIfNeeded().
    LaunchedEffect(Unit) { viewModel.refreshIfNeeded() }

    // Notification permission is asked via its own dedicated interstitial right after phone
    // collection / sign-in — see SessionViewModel.resolvePostAuthGate() — rather than here.

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
                    val trialStatus = uiState.trialStatus
                    when {
                        activeSale != null -> SaleDayBanner(saleName = activeSale.name, onClick = onOpenPricing)
                        trialStatus is TrialStatus.Expired -> TrialEndedBanner(onClick = onOpenPricing)
                        trialStatus is TrialStatus.Extended -> TrialExtendedBanner(daysLeft = trialStatus.daysLeft, onClick = onOpenPricing)
                        // Before their first quiz exists, a brand-new trial account has had zero
                        // chance to see any value yet — show the neutral feature-chip welcome
                        // instead of the countdown/"View Plans" banner, so the very first thing a
                        // new user sees on Dashboard isn't a pricing pitch. The plain trial banner
                        // takes back over from their second quiz-owning visit onward.
                        trialStatus is TrialStatus.Active && uiState.totalQuizzes == 0 -> WelcomeBanner()
                        trialStatus is TrialStatus.Active -> TrialActiveBanner(daysLeft = trialStatus.daysLeft, onClick = onOpenPricing)
                        else -> WelcomeBanner()
                    }
                    Spacer(Modifier.height(20.dp))
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text("Quick Actions", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            QuickActionButton(
                                icon = Icons.Default.QrCodeScanner,
                                label = "Scanner",
                                iconBg = StatPurpleBg,
                                iconTint = StatPurpleIcon,
                                onClick = { onScannerTapped() },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                icon = Icons.Default.NoteAdd,
                                label = "Create Quiz",
                                iconBg = BrandIndigoLight,
                                iconTint = BrandIndigo,
                                onClick = { viewModel.onCreateQuizClick(onCreateQuiz) },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                icon = Icons.Default.Construction,
                                label = "Tools",
                                iconBg = StatAmberBg,
                                iconTint = StatAmberIcon,
                                onClick = onOpenTools,
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                icon = Icons.Default.School,
                                label = "Classes",
                                iconBg = StatTealBg,
                                iconTint = StatTealIcon,
                                onClick = onOpenClasses,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            QuickActionButton(
                                icon = Icons.Default.Print,
                                label = "Offline Exam",
                                iconBg = StatRoseBg,
                                iconTint = StatRoseIcon,
                                onClick = onOpenOfflineExamList,
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                icon = Icons.Default.CardMembership,
                                label = "Certificate Design",
                                iconBg = StatBlueBg,
                                iconTint = StatBlueIcon,
                                onClick = onOpenCertificateDesigner,
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                icon = Icons.Default.Group,
                                label = "Learners",
                                iconBg = StatGreenBg,
                                iconTint = StatGreenIcon,
                                onClick = onOpenLearners,
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionButton(
                                icon = Icons.Default.FileDownload,
                                label = "Import Questions",
                                iconBg = StatRedBg,
                                iconTint = StatRedIcon,
                                onClick = onOpenImportQuestions,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }

                // "View Feature" playground — an onboarding/upsell nudge, so it's only worth
                // showing to accounts that haven't bought a license yet, and only until its X has
                // been dismissed twice (see DashboardViewModel.onDismissFeatureTourBanner).
                if (uiState.trialStatus !is TrialStatus.Premium && !uiState.featureTourBannerDismissed) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            FeatureTourBanner(onClick = onOpenFeatureTour, onDismiss = viewModel::onDismissFeatureTourBanner)
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }

                uiState.errorMessage?.let { message ->
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            ErrorBanner(message = message, onRetry = viewModel::refresh)
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                if (uiState.recentQuizzes.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Recent Quiz",
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "View all",
                                    color = BrandIndigo,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable(onClick = onOpenQuizzes)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            uiState.recentQuizzes.forEach { quiz ->
                                RecentQuizCard(
                                    quiz = quiz,
                                    questionCount = uiState.recentQuizQuestionCounts[quiz.id],
                                    onClick = { onOpenQuiz(quiz.id) }
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }

                item {
                    var showDateSheet by remember { mutableStateOf(false) }
                    val customStart = uiState.customRangeStart
                    val customEnd = uiState.customRangeEnd
                    val rangeLabel = if (uiState.selectedRange == DashboardDateRange.CUSTOM && customStart != null && customEnd != null) {
                        // customRangeEnd is stored exclusive (see DashboardDateRangeSheet's KDoc) —
                        // step back a day so the label shows the inclusive "To" date the user picked.
                        "${formatShortDate(customStart)} – ${formatShortDate(customEnd - 1.days)}"
                    } else {
                        uiState.selectedRange.label
                    }
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceWhite, RoundedCornerShape(50))
                                .border(1.dp, BorderGray, RoundedCornerShape(50))
                                .clickable { showDateSheet = true }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                rangeLabel,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextSecondary)
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                    if (showDateSheet) {
                        DashboardDateRangeSheet(
                            selectedRange = uiState.selectedRange,
                            customStart = uiState.customRangeStart,
                            customEnd = uiState.customRangeEnd,
                            onSelectPreset = viewModel::onRangeSelected,
                            onApplyCustomRange = viewModel::onCustomRangeSelected,
                            onDismiss = { showDateSheet = false }
                        )
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
                        Spacer(Modifier.height(14.dp))
                        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            StatTile(
                                icon = Icons.Default.Flag,
                                iconBg = StatRedBg,
                                iconTint = StatRedIcon,
                                label = "Reported Questions",
                                value = uiState.reportedQuestionsCount.toString(),
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable(onClick = onOpenReportedQuestions)
                            )
                            StatTile(
                                icon = Icons.Default.Group,
                                iconBg = StatTealBg,
                                iconTint = StatTealIcon,
                                label = "Learners",
                                value = uiState.learnersCount.toString(),
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).clickable(onClick = onOpenLearners)
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

    if (uiState.showTrialPaywall) {
        TrialPaywallSheet(onDismiss = viewModel::dismissTrialPaywall, onViewPlans = onOpenPricing)
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun RecentQuizCard(quiz: Quiz, questionCount: Int?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(StatPurpleBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = StatPurpleIcon, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                quiz.title.ifBlank { "Untitled quiz" },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${questionCount ?: "-"} Questions", color = TextSecondary, fontSize = 12.sp)
                Text("  •  ", color = TextSecondary, fontSize = 12.sp)
                Text(
                    if (quiz.isClosed) "Closed" else "Published",
                    color = if (quiz.isClosed) TextSecondary else SuccessGreen,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
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

/**
 * Same "premium" visual recipe as [SaleDayBanner]/[TrialActiveBanner] — a large faint watermark
 * icon plus a couple of sparkle accents behind the content — just in the plain indigo brand
 * gradient instead of red, so a fully-settled premium account's Dashboard still reads as polished
 * rather than a flat, bare gradient once there's no sale/trial messaging left to show.
 */
@Composable
private fun WelcomeBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(Brush.horizontalGradient(listOf(BrandIndigoDark, BrandIndigo)))
    ) {
        Icon(
            Icons.Default.WorkspacePremium,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.10f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 30.dp, y = 4.dp)
                .size(150.dp)
                .rotate(-18f)
        )
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-38).dp, y = 50.dp)
                .size(15.dp)
        )
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-16).dp, y = (-62).dp)
                .size(10.dp)
        )

        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 26.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Welcome to Yuno LMS", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            }
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

/** Minimal "which quiz is this scan for" picker for the Scanner quick action -- a plain list, not a
 *  full search/filter UI, since a teacher's quiz list here is expected to be short enough to just
 *  scroll (mirrors CreateGroupDialog's plain-AlertDialog style rather than inventing a new sheet). */
@Composable
private fun OmrQuizPickerDialog(quizzes: List<Quiz>, onDismiss: () -> Unit, onQuizSelected: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scan Answer Sheets For…") },
        text = {
            if (quizzes.isEmpty()) {
                Text("Create a quiz first to scan answer sheets for it.", color = TextSecondary)
            } else {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(quizzes, key = { it.id }) { quiz ->
                        Text(
                            quiz.title,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onQuizSelected(quiz.id) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
