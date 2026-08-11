package com.quizmaker.android.ui.quizlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.quizmaker.android.core.theme.AiCardBg
import com.quizmaker.android.core.theme.AiCardBorder
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.data.model.QuizAiSummary
import com.quizmaker.android.ui.common.AiSummaryCard
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.QuizFeaturesBanner
import com.quizmaker.android.ui.common.ShareQuizSheet
import com.quizmaker.android.ui.common.TrialPaywallSheet
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.formatShortDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizListScreen(
    onOpenQuiz: (String) -> Unit,
    onCreateQuiz: () -> Unit,
    onOpenAi: () -> Unit,
    onViewLeaderboard: (String) -> Unit,
    onOpenMasterPaper: (String) -> Unit,
    onOpenQuizAnalysis: (String) -> Unit,
    onOpenQuizDetailView: (String) -> Unit,
    onEditQuiz: (String) -> Unit,
    onOpenPricing: () -> Unit,
    viewModel: QuizListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var quizPendingDelete by remember { mutableStateOf<Quiz?>(null) }
    var quizPendingShare by remember { mutableStateOf<Quiz?>(null) }
    var quizPendingMenu by remember { mutableStateOf<Quiz?>(null) }
    var quizPendingLinkToClass by remember { mutableStateOf<Quiz?>(null) }

    // A newly-created quiz (QuizCreatedScreen's Done button) or an edit made elsewhere pops back
    // to this same screen instance — hiltViewModel() keeps the same ViewModel alive across that
    // round trip, so its one-shot init{} load never sees the change on its own. Reload on every
    // resume after the first (which already happened via init{}), same fix as Question Bank's.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var isFirstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isFirstResume) isFirstResume = false else viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The bottom nav bar's own Scaffold (NavGraph) already reserves the system nav-bar inset, so
    // this only adds the top one — unlike Dashboard, this screen has no full-bleed banner to
    // manually paint behind the status bar, so without this the Create Quiz button rendered
    // right against it with no clearance.
    Scaffold(containerColor = AppBackground, contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    GradientButton(
                        text = "Create Quiz",
                        onClick = { viewModel.onCreateQuizClick(onCreateQuiz) },
                        leadingIcon = Icons.Default.NoteAdd,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, BrandIndigo, RoundedCornerShape(50))
                            .clickable(onClick = onOpenAi)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AI", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Box(modifier = Modifier.fillMaxSize()) {
            LoadingCrossfade(
                isLoading = uiState.isLoading,
                loadingContent = {
                    ListScreenSkeleton(rowCount = 5, rowLineWidths = listOf(160.dp, 110.dp, 200.dp))
                }
            ) {
                val listState = rememberLazyListState()
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        layoutInfo.totalItemsCount > 0 && lastVisible >= layoutInfo.totalItemsCount - 5
                    }
                }
                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore) viewModel.loadMore()
                }

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { QuizFeaturesBanner() }

                    item {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = { Text("Search quizzes...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    uiState.errorMessage?.let {
                        item { ErrorBanner(message = it, onRetry = viewModel::refresh) }
                    }

                    if (uiState.filteredQuizzes.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.NoteAdd,
                                title = if (uiState.allQuizzes.isEmpty()) "No quizzes yet" else "No matches",
                                subtitle = if (uiState.allQuizzes.isEmpty()) "Create your first quiz to get started." else "Try a different search term."
                            )
                        }
                    } else {
                        items(uiState.filteredQuizzes, key = { it.id }) { quiz ->
                            QuizListRow(
                                quiz = quiz,
                                questionCount = uiState.questionCounts[quiz.id],
                                responseCount = uiState.responseCounts[quiz.id],
                                aiSummary = uiState.aiSummaries[quiz.id],
                                isAiSummaryExpanded = quiz.id in uiState.expandedAiSummaryQuizIds,
                                isAiSummaryLoading = quiz.id in uiState.loadingAiSummaryQuizIds,
                                isDeleting = quiz.id == uiState.deletingQuizId,
                                onToggleAiSummary = { viewModel.onToggleAiSummary(quiz.id) },
                                onClick = { onOpenQuiz(quiz.id) },
                                onViewLeaderboard = { onViewLeaderboard(quiz.id) },
                                onOpenQuizDetailView = { onOpenQuizDetailView(quiz.id) },
                                onShare = { quizPendingShare = quiz },
                                onMenuClick = { quizPendingMenu = quiz },
                                onEditQuiz = { onEditQuiz(quiz.id) }
                            )
                        }
                    }
                    if (uiState.isLoadingMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BrandIndigo, strokeWidth = 2.dp)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, AppBackground)))
                )
            }
        }
    }

    if (uiState.showTrialPaywall) {
        TrialPaywallSheet(onDismiss = viewModel::dismissTrialPaywall, onViewPlans = onOpenPricing)
    }

    quizPendingShare?.let { quiz ->
        ShareQuizSheet(quizTitle = quiz.title, shareUrl = quiz.shareUrl, onDismiss = { quizPendingShare = null })
    }

    quizPendingDelete?.let { quiz ->
        AlertDialog(
            onDismissRequest = { quizPendingDelete = null },
            title = { Text("Delete \"${quiz.title}\"?") },
            text = { Text("This permanently deletes the quiz and all of its responses.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteQuiz(quiz.id)
                    quizPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { quizPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    quizPendingMenu?.let { quiz ->
        QuizActionsSheet(
            quiz = quiz,
            onDismiss = { quizPendingMenu = null },
            onDuplicate = { viewModel.duplicateQuiz(quiz.id) },
            onOpenMasterPaper = { onOpenMasterPaper(quiz.id) },
            onOpenQuizAnalysis = { onOpenQuizAnalysis(quiz.id) },
            onOpenQuizDetailView = { onOpenQuizDetailView(quiz.id) },
            onLinkToClass = { quizPendingLinkToClass = quiz },
            onClose = { viewModel.closeQuiz(quiz.id) },
            onRequestDelete = { quizPendingDelete = quiz }
        )
    }

    quizPendingLinkToClass?.let { quiz ->
        LinkToClassSheet(quiz = quiz, onDismiss = { quizPendingLinkToClass = null })
    }
}

@Composable
private fun QuizListRow(
    quiz: Quiz,
    questionCount: Int?,
    responseCount: Int?,
    aiSummary: QuizAiSummary?,
    isAiSummaryExpanded: Boolean,
    isAiSummaryLoading: Boolean,
    isDeleting: Boolean,
    onToggleAiSummary: () -> Unit,
    onClick: () -> Unit,
    onViewLeaderboard: () -> Unit,
    onOpenQuizDetailView: () -> Unit,
    onShare: () -> Unit,
    onMenuClick: () -> Unit,
    onEditQuiz: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .elevatedSurface(shape = RoundedCornerShape(20.dp))
                .padding(18.dp)
                .alpha(if (isDeleting) 0.4f else 1f)
        ) {
            Column(modifier = Modifier.clickable(enabled = !isDeleting, onClick = onClick)) {
                Text(quiz.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(formatShortDate(quiz.createdAt), color = TextSecondary, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${questionCount ?: "-"} questions", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("${responseCount ?: "-"} submissions", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AiSummaryIconButton(isExpanded = isAiSummaryExpanded, onClick = onToggleAiSummary)
                CircleIconButton(icon = Icons.Default.Edit, onClick = onEditQuiz)
                CircleIconButton(icon = Icons.Default.EmojiEvents, onClick = onViewLeaderboard)
                CircleIconButton(icon = Icons.Default.Visibility, onClick = onOpenQuizDetailView)
                CircleIconButton(icon = Icons.Default.Share, onClick = onShare)
                CircleIconButton(icon = Icons.Default.MoreVert, onClick = onMenuClick)
            }
            AnimatedVisibility(visible = isAiSummaryExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    AiSummaryCard(summary = aiSummary, isLoading = isAiSummaryLoading)
                }
            }
        }
        if (isDeleting) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(50))
                    .background(SurfaceWhite)
                    .border(1.dp, ErrorRed, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ErrorRed, strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Deleting…", color = ErrorRed, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
        }
    }
}

/** The "AI" icon that toggles a quiz card's AI Summary open/closed — same light tint as the
 *  AiSummaryCard it opens (see its KDoc: this is branded as AI but is just cached SQL stats),
 *  not a bold fill, so it reads as a subtle toggle rather than competing with the row's other icons. */
@Composable
private fun AiSummaryIconButton(isExpanded: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
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
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * A screen-covering, bottom-anchored actions panel — deliberately built on [Dialog] instead of
 * Material3's ModalBottomSheet so it can slide up without also picking up ModalBottomSheet's
 * drag-to-dismiss/drag-to-resize gesture, which the user explicitly didn't want here.
 */
@Composable
private fun QuizActionsSheet(
    quiz: Quiz,
    onDismiss: () -> Unit,
    onDuplicate: () -> Unit,
    onOpenMasterPaper: () -> Unit,
    onOpenQuizAnalysis: () -> Unit,
    onOpenQuizDetailView: () -> Unit,
    onLinkToClass: () -> Unit,
    onClose: () -> Unit,
    onRequestDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        val noRipple = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(indication = null, interactionSource = noRipple, onClick = onDismiss)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(SurfaceWhite)
                        .clickable(indication = null, interactionSource = noRipple, onClick = {})
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                ) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(BorderGray)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        quiz.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(8.dp))

                    SheetActionRow("Master Paper", Icons.Default.Description) { onDismiss(); onOpenMasterPaper() }
                    SheetActionRow("Question Performance", Icons.Default.BarChart) { onDismiss(); onOpenQuizAnalysis() }
                    SheetActionRow("Quiz Detail View", Icons.Default.Assignment) { onDismiss(); onOpenQuizDetailView() }
                    SheetActionRow("Duplicate Quiz", Icons.Default.ContentCopy) { onDismiss(); onDuplicate() }
                    SheetActionRow("Link to Class", Icons.Default.GroupAdd) { onDismiss(); onLinkToClass() }
                    if (!quiz.isClosed) {
                        SheetActionRow("Close Quiz", Icons.Default.Lock) { onDismiss(); onClose() }
                    }
                    SheetActionRow("Delete Quiz", Icons.Default.Delete, tint = ErrorRed) { onDismiss(); onRequestDelete() }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkToClassSheet(quiz: Quiz, onDismiss: () -> Unit, viewModel: LinkToClassViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(quiz.id) { viewModel.load(quiz.id) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = SurfaceWhite) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Link to Class", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Add \"${quiz.title}\" to one or more classes", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(16.dp))
            when {
                uiState.isLoading -> Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandIndigo)
                }
                uiState.classes.isEmpty() -> Text(
                    "You don't have any classes yet — create one below.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.classes.forEach { quizClass ->
                        val isLinked = quizClass.id in uiState.linkedClassIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .elevatedSurface(shape = RoundedCornerShape(14.dp), elevation = 2.dp, color = if (isLinked) BrandIndigoLight else SurfaceWhite)
                                .clickable { viewModel.toggleClass(quizClass.id) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(quizClass.name, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (isLinked) {
                                Icon(Icons.Default.Check, contentDescription = "Linked", tint = BrandIndigo)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "+ New Class",
                color = BrandIndigo,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = viewModel::openCreateDialog)
            )
        }
    }

    if (uiState.isCreateDialogOpen) {
        CreateAndLinkClassDialog(
            isCreating = uiState.isCreating,
            onDismiss = viewModel::dismissCreateDialog,
            onCreate = viewModel::createAndLinkClass
        )
    }
}

@Composable
private fun CreateAndLinkClassDialog(isCreating: Boolean, onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Class") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Class name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(enabled = !isCreating, onClick = { onCreate(name) }) {
                Text(if (isCreating) "Creating…" else "Create & Link", color = BrandIndigo, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SheetActionRow(label: String, icon: ImageVector, tint: Color = TextPrimary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = tint, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

@Composable
private fun CircleIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(1.dp, BorderGray, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
    }
}
