package com.quizmaker.android.ui.questionbank

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionDifficulty
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.ui.common.AiQuestionsBanner
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.FilledPill
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.OutlinedPill
import com.quizmaker.android.ui.common.QuestionTypeOption
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.formatShortDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionBankScreen(
    onOpenAi: () -> Unit = {},
    onCreateQuizFromSelection: (List<String>) -> Unit = {},
    viewModel: QuestionBankViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var questionPendingDelete by remember { mutableStateOf<Question?>(null) }

    // The bottom nav bar's own Scaffold (NavGraph) already reserves the system nav-bar inset;
    // reserving it again here would leave a redundant empty strip above the tab bar.
    Scaffold(containerColor = AppBackground, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(icon = Icons.Default.Refresh, onClick = { viewModel.loadQuestions() })
                    RoundIconButton(icon = Icons.Default.FilterList, onClick = { viewModel.openFilterSheet() })
                    RoundIconButton(
                        icon = Icons.Default.ChecklistRtl,
                        onClick = { viewModel.toggleSelectionMode() },
                        isActive = uiState.isSelectionMode
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, BrandIndigo, RoundedCornerShape(50))
                            .clickable(onClick = onOpenAi)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AI", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    GradientButton(
                        text = "New",
                        onClick = { viewModel.startNewQuestion() },
                        leadingIcon = Icons.Default.Add,
                        height = 44.dp,
                        modifier = Modifier.width(110.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("Search questions...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
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
                            "${uiState.selectedQuestionIds.size} of ${QuestionBankViewModel.MAX_SELECTABLE_QUESTIONS} selected",
                            color = BrandIndigo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            "Cancel",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable(onClick = viewModel::exitSelectionMode)
                        )
                    }
                } else {
                    Text(
                        "Showing ${uiState.filtered.size} of ${uiState.questions.size} questions",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                Spacer(Modifier.height(12.dp))

                uiState.errorMessage?.let {
                    ErrorBanner(message = it, onRetry = viewModel::loadQuestions)
                    Spacer(Modifier.height(12.dp))
                }
            }

            LoadingCrossfade(
                isLoading = uiState.isLoading,
                loadingContent = { ListScreenSkeleton(rowCount = 6, rowLineWidths = listOf(240.dp, 140.dp)) }
            ) {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { AiQuestionsBanner(onClick = onOpenAi) }

                    if (uiState.filtered.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.Info,
                                title = if (uiState.questions.isEmpty()) "No questions yet" else "No matches",
                                subtitle = if (uiState.questions.isEmpty()) "Tap New to add your first question." else "Try a different search term."
                            )
                        }
                    } else {
                        items(uiState.filtered, key = { it.id }) { question ->
                            QuestionCard(
                                question = question,
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = question.id in uiState.selectedQuestionIds,
                                onToggleSelect = { viewModel.toggleQuestionSelection(question.id) },
                                onEditClick = { viewModel.startEditQuestion(question) },
                                onDeleteClick = { questionPendingDelete = question }
                            )
                        }
                        item { Spacer(Modifier.height(if (uiState.isSelectionMode) 100.dp else 80.dp)) }
                    }
                }
            }
        }

        if (uiState.isSelectionMode && uiState.selectedQuestionIds.isNotEmpty()) {
            GradientButton(
                text = "Create Quiz (${uiState.selectedQuestionIds.size})",
                onClick = { onCreateQuizFromSelection(uiState.selectedQuestionIds.toList()) },
                leadingIcon = Icons.Default.NoteAdd,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .widthIn(min = 220.dp)
            )
        }
        }
    }

    if (uiState.isFilterSheetOpen) {
        FilterSheet(uiState = uiState, viewModel = viewModel)
    }

    questionPendingDelete?.let { question ->
        AlertDialog(
            onDismissRequest = { questionPendingDelete = null },
            title = { Text("Delete this question?") },
            text = { Text("This permanently removes it from your question bank and any quizzes using it.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteQuestion(question.id)
                    questionPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { questionPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (uiState.draft != null) {
        NewQuestionSheet(viewModel = viewModel)
    }
}

@Composable
private fun RoundIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, isActive: Boolean = false) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(1.dp, if (isActive) BrandIndigo else BorderGray, CircleShape)
            .then(if (isActive) Modifier.background(BrandIndigoLight, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (isActive) BrandIndigo else TextPrimary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun QuestionCard(
    question: Question,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(20.dp))
            .then(if (isSelectionMode) Modifier.clickable(onClick = onToggleSelect) else Modifier)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (isSelectionMode) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
                Spacer(Modifier.width(4.dp))
            }
            Text(question.text, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledPill(text = questionTypeLabel(question.type))
            question.difficulty?.let { OutlinedPill(text = it.value.replaceFirstChar { c -> c.uppercase() }, borderColor = ErrorRed, contentColor = ErrorRed) }
        }
        if (!question.createdAt.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(formatShortDate(question.createdAt), color = TextSecondary, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (question.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    question.tags.forEach { OutlinedPill(text = it) }
                }
            } else {
                Spacer(Modifier)
            }
            if (!isSelectionMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallIconButton(icon = Icons.Default.Edit, tint = BrandIndigo, onClick = onEditClick)
                    SmallIconButton(icon = Icons.Default.Delete, tint = ErrorRed, onClick = onDeleteClick)
                }
            }
        }
    }
}

@Composable
private fun SmallIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .border(1.dp, BorderGray, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

private fun questionTypeLabel(type: QuestionType): String = when (type) {
    QuestionType.SINGLE_CHOICE -> "Single Choice"
    QuestionType.MULTI_CHOICE -> "Multiple Choice"
    QuestionType.FREE_TEXT -> "Free Text"
    QuestionType.FILL_IN_BLANK -> "Fill in the Blanks"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(uiState: QuestionBankUiState, viewModel: QuestionBankViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTag by remember { mutableStateOf(uiState.tagFilter) }
    var selectedDifficulty by remember { mutableStateOf(uiState.difficultyFilter) }

    ModalBottomSheet(onDismissRequest = viewModel::closeFilterSheet, sheetState = sheetState, containerColor = SurfaceWhite) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Filter Questions", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Narrow down your question bank", color = TextSecondary, fontSize = 13.sp)
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
            Text("DIFFICULTY", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuestionDifficulty.entries.forEach { difficulty ->
                    FilterChip(
                        label = difficulty.value.replaceFirstChar { c -> c.uppercase() },
                        selected = selectedDifficulty == difficulty
                    ) {
                        selectedDifficulty = if (selectedDifficulty == difficulty) null else difficulty
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            GradientButton(
                text = "Apply Filter",
                onClick = { viewModel.applyFilter(selectedTag, selectedDifficulty) },
                modifier = Modifier.fillMaxWidth()
            )
            if (selectedTag != null || selectedDifficulty != null) {
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
                            selectedDifficulty = null
                            viewModel.applyFilter(null, null)
                        },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewQuestionSheet(viewModel: QuestionBankViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val draft = uiState.draft ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = viewModel::dismissDraft, sheetState = sheetState, containerColor = SurfaceWhite) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    val isEditing = draft.editingQuestionId != null
                    Text(
                        if (isEditing) "Edit Question" else "Create New Question",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Text(
                        if (isEditing) "Update this question in your bank" else "Add a question to your bank",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = viewModel::dismissDraft) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("QUESTION TYPE", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuestionTypeOption(
                    label = "Single Choice",
                    subtitle = "One correct answer",
                    selected = draft.type == QuestionType.SINGLE_CHOICE,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.updateDraft { it.copy(type = QuestionType.SINGLE_CHOICE) } }
                )
                QuestionTypeOption(
                    label = "Multiple Choice",
                    subtitle = "Many correct answers",
                    selected = draft.type == QuestionType.MULTI_CHOICE,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.updateDraft { it.copy(type = QuestionType.MULTI_CHOICE) } }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuestionTypeOption(
                    label = "Free Text",
                    subtitle = "Open written response",
                    selected = draft.type == QuestionType.FREE_TEXT,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.updateDraft { it.copy(type = QuestionType.FREE_TEXT) } }
                )
                QuestionTypeOption(
                    label = "Fill in the Blanks",
                    subtitle = "Auto-graded text",
                    selected = draft.type == QuestionType.FILL_IN_BLANK,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.updateDraft { it.copy(type = QuestionType.FILL_IN_BLANK) } }
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("QUESTION TEXT", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.text,
                onValueChange = { text -> viewModel.updateDraft { it.copy(text = text) } },
                placeholder = { Text("Type your question here...") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            if (draft.type == QuestionType.SINGLE_CHOICE || draft.type == QuestionType.MULTI_CHOICE) {
                Spacer(Modifier.height(20.dp))
                Text("OPTIONS", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(8.dp))
                draft.options.forEachIndexed { index, optionText ->
                    Row(modifier = Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (draft.type == QuestionType.SINGLE_CHOICE) {
                            RadioButton(
                                selected = draft.correctOptionIndex == index,
                                onClick = { viewModel.updateDraft { it.copy(correctOptionIndex = index) } }
                            )
                        } else {
                            Checkbox(
                                checked = index in draft.correctOptionIndices,
                                onCheckedChange = { checked ->
                                    viewModel.updateDraft {
                                        val updated = if (checked) it.correctOptionIndices + index else it.correctOptionIndices - index
                                        it.copy(correctOptionIndices = updated)
                                    }
                                }
                            )
                        }
                        OutlinedTextField(
                            value = optionText,
                            onValueChange = { text ->
                                viewModel.updateDraft { it.copy(options = it.options.toMutableList().also { list -> list[index] = text }) }
                            },
                            placeholder = { Text("Option ${index + 1}") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (draft.options.size < 6) {
                        Text(
                            "+ Add option",
                            color = BrandIndigo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable {
                                viewModel.updateDraft { it.copy(options = it.options + "") }
                            }
                        )
                    }
                    if (draft.options.size > 2) {
                        Text(
                            "− Remove option",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable {
                                viewModel.updateDraft {
                                    val newOptions = it.options.dropLast(1)
                                    it.copy(
                                        options = newOptions,
                                        correctOptionIndex = it.correctOptionIndex.coerceAtMost(newOptions.size - 1),
                                        correctOptionIndices = it.correctOptionIndices.filter { i -> i < newOptions.size }.toSet()
                                    )
                                }
                            }
                        )
                    }
                }
            } else if (draft.type == QuestionType.FILL_IN_BLANK || draft.type == QuestionType.FREE_TEXT) {
                Spacer(Modifier.height(16.dp))
                Text("ANSWER", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draft.freeTextAnswer,
                    onValueChange = { text -> viewModel.updateDraft { it.copy(freeTextAnswer = text) } },
                    placeholder = { Text("Expected answer") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, BorderGray, RoundedCornerShape(50))
                        .clickable(onClick = viewModel::dismissDraft),
                    contentAlignment = Alignment.Center
                ) { Text("Cancel", color = TextPrimary, fontWeight = FontWeight.Bold) }

                Box(modifier = Modifier.weight(1f)) {
                    GradientButton(
                        text = if (draft.editingQuestionId != null) "Update" else "Save",
                        onClick = { viewModel.saveDraft(keepEditing = false) },
                        loading = uiState.isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
