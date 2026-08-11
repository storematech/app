package com.quizmaker.android.ui.revision

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.RevisionItem
import com.quizmaker.android.data.model.RevisionStatus
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.FilledPill
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.OutlinedPill
import com.quizmaker.android.ui.common.QuestionEditSheet
import com.quizmaker.android.ui.common.elevatedSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionScreen(
    onNavigateBack: () -> Unit,
    onCreateRetest: (List<String>) -> Unit,
    viewModel: RevisionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Revision", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = viewModel::openFilterSheet) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = { Text("Search bookmarked questions...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    if (uiState.isSelectionMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${uiState.selectedIds.size} of ${RevisionViewModel.MAX_SELECTABLE} selected",
                                color = BrandIndigo,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    "Select All",
                                    color = BrandIndigo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable(onClick = viewModel::selectAll)
                                )
                                Text(
                                    "Cancel",
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable(onClick = viewModel::exitSelectionMode)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Showing ${uiState.filtered.size} of ${uiState.items.size} bookmarked", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                "Select",
                                color = BrandIndigo,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.clickable(onClick = viewModel::toggleSelectionMode)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    uiState.errorMessage?.let {
                        ErrorBanner(message = it, onRetry = viewModel::refresh)
                        Spacer(Modifier.height(12.dp))
                    }
                }

                LoadingCrossfade(
                    isLoading = uiState.isLoading,
                    loadingContent = { ListScreenSkeleton(rowCount = 6, rowLineWidths = listOf(220.dp, 140.dp)) }
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (uiState.filtered.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Default.Bookmark,
                                    title = if (uiState.items.isEmpty()) "Nothing bookmarked yet" else "No matches",
                                    subtitle = if (uiState.items.isEmpty()) {
                                        "Add questions for revision from a quiz's Question Performance screen."
                                    } else {
                                        "Try a different search or filter."
                                    }
                                )
                            }
                        } else {
                            items(uiState.filtered, key = { it.id }) { item ->
                                RevisionCard(
                                    item = item,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isSelected = item.id in uiState.selectedIds,
                                    onToggleSelect = { viewModel.toggleSelection(item.id) },
                                    onEditQuestion = { viewModel.startEditQuestion(item.question) }
                                )
                            }
                            item { Spacer(Modifier.height(if (uiState.isSelectionMode) 150.dp else 80.dp)) }
                        }
                    }
                }
            }

            if (uiState.isSelectionMode && uiState.selectedIds.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(SurfaceWhite)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedActionButton(
                            text = if (uiState.isDeleting) "Deleting…" else "Delete",
                            icon = Icons.Default.Delete,
                            tint = ErrorRed,
                            onClick = viewModel::deleteSelected,
                            loading = uiState.isDeleting,
                            enabled = !uiState.isMarkingDone,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedActionButton(
                            text = if (uiState.isMarkingDone) "Marking…" else "Mark Done",
                            icon = Icons.Default.CheckCircle,
                            tint = SuccessGreen,
                            onClick = viewModel::markSelectedDone,
                            loading = uiState.isMarkingDone,
                            enabled = !uiState.isDeleting,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    GradientButton(
                        text = "Create A Re-Test (${uiState.selectedIds.size})",
                        onClick = { onCreateRetest(viewModel.createRetest()) },
                        leadingIcon = Icons.Default.NoteAdd,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (uiState.isFilterSheetOpen) {
        RevisionFilterSheet(uiState = uiState, viewModel = viewModel)
    }

    uiState.draft?.let { draft ->
        QuestionEditSheet(
            draft = draft,
            isSaving = uiState.isSavingDraft,
            onUpdateDraft = viewModel::updateDraft,
            onSave = viewModel::saveDraft,
            onDismiss = viewModel::dismissDraft
        )
    }
}

@Composable
private fun OutlinedActionButton(
    text: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, tint, RoundedCornerShape(50))
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = tint, strokeWidth = 2.dp)
            } else {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(text, color = tint, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

private const val QUESTION_TEXT_MAX_CHARS = 200

@Composable
private fun RevisionCard(
    item: RevisionItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onEditQuestion: () -> Unit
) {
    val displayText = if (item.question.text.length > QUESTION_TEXT_MAX_CHARS) {
        item.question.text.take(QUESTION_TEXT_MAX_CHARS) + "…"
    } else {
        item.question.text
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(20.dp))
            .then(if (isSelectionMode) Modifier.clickable(onClick = onToggleSelect) else Modifier.clickable(onClick = onEditQuestion))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
                Spacer(Modifier.width(4.dp))
            }
            Text(displayText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary, modifier = Modifier.weight(1f))
            if (item.status == RevisionStatus.DONE) {
                FilledPill(text = "Done", containerColor = SuccessGreen)
            } else {
                OutlinedPill(text = "Pending")
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item.quizTitle?.let { OutlinedPill(text = it, borderColor = BrandIndigo, contentColor = BrandIndigo) }
            item.question.tags.forEach { OutlinedPill(text = it) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RevisionFilterSheet(uiState: RevisionUiState, viewModel: RevisionViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTag by remember { mutableStateOf(uiState.tagFilter) }
    var selectedQuiz by remember { mutableStateOf(uiState.quizFilter) }

    ModalBottomSheet(onDismissRequest = viewModel::closeFilterSheet, sheetState = sheetState, containerColor = SurfaceWhite) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Filter Revision", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Narrow down your bookmarked questions", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            Text("TAG", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(10.dp))
            if (uiState.availableTags.isEmpty()) {
                Text("No tags yet", color = TextSecondary, fontSize = 13.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.availableTags) { tag ->
                        FilterChip(label = tag, selected = selectedTag == tag) {
                            selectedTag = if (selectedTag == tag) null else tag
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("QUIZ", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(10.dp))
            if (uiState.availableQuizzes.isEmpty()) {
                Text("No quizzes yet", color = TextSecondary, fontSize = 13.sp)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.availableQuizzes) { quiz ->
                        FilterChip(label = quiz, selected = selectedQuiz == quiz) {
                            selectedQuiz = if (selectedQuiz == quiz) null else quiz
                        }
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            GradientButton(
                text = "Apply Filter",
                onClick = { viewModel.applyFilter(selectedTag, selectedQuiz) },
                modifier = Modifier.fillMaxWidth()
            )
            if (selectedTag != null || selectedQuiz != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Clear filters",
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedTag = null
                            selectedQuiz = null
                            viewModel.applyFilter(null, null)
                        },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) BrandIndigo else AppBackground)
            .border(1.dp, if (selected) BrandIndigo else BorderGray, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(label, color = if (selected) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
