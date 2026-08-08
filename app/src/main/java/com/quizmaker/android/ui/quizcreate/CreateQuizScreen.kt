package com.quizmaker.android.ui.quizcreate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.StatGreenIcon
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionDifficulty
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.FilledPill
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.OutlinedPill
import com.quizmaker.android.ui.common.QuestionTypeOption
import com.quizmaker.android.ui.common.SettingsRow
import com.quizmaker.android.ui.common.elevatedSurface

private val STEP_LABELS = listOf("Details", "Questions", "Settings", "Review")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizScreen(
    onNavigateBack: () -> Unit,
    onQuizCreated: (String) -> Unit,
    viewModel: CreateQuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Both new-quiz and edit-quiz completion just hand off to the caller — for a new quiz, that's
    // NavGraph navigating to the dedicated Screen.QuizCreated route (QR/share/Master Paper), not
    // an inline screen here (there used to be one; removed so only one "quiz created" screen shows).
    LaunchedEffect(uiState.resultQuiz) {
        uiState.resultQuiz?.let { onQuizCreated(it.id) }
    }

    if (uiState.isLoadingForEdit) {
        Scaffold(containerColor = AppBackground) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandIndigo)
            }
        }
        return
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            Column(modifier = Modifier.background(AppBackground)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (uiState.step == CreateQuizStep.DETAILS) onNavigateBack() else viewModel.goBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Spacer(Modifier.height(4.dp))
                StepIndicator(currentIndex = uiState.step.ordinal)
                Spacer(Modifier.height(12.dp))
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.step != CreateQuizStep.DETAILS) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, BorderGray, RoundedCornerShape(50))
                            .clickable(onClick = viewModel::goBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Back", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                val canProceed = when (uiState.step) {
                    CreateQuizStep.DETAILS -> uiState.canGoNextFromDetails
                    CreateQuizStep.QUESTIONS -> uiState.canGoNextFromQuestions
                    CreateQuizStep.SETTINGS -> true
                    CreateQuizStep.REVIEW -> !uiState.isSubmitting
                }
                val reviewLabel = if (uiState.isEditMode) "Save Changes" else "Create Quiz"
                GradientButton(
                    text = if (uiState.step == CreateQuizStep.REVIEW) reviewLabel else "Next",
                    onClick = { if (uiState.step == CreateQuizStep.REVIEW) viewModel.submit() else viewModel.goNext() },
                    enabled = canProceed,
                    loading = uiState.isSubmitting,
                    modifier = Modifier.weight(if (uiState.step == CreateQuizStep.DETAILS) 1f else 1.4f)
                )
            }
        }
    ) { padding ->
        // The Questions step renders its list in a LazyColumn (can be hundreds of questions —
        // a plain Column+forEach here was composing/laying out every row synchronously and
        // froze the UI for a second or two right after tapping Next). A LazyColumn needs bounded
        // height, so unlike the other steps it isn't nested inside the outer verticalScroll.
        if (uiState.step == CreateQuizStep.QUESTIONS) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
                uiState.errorMessage?.let {
                    ErrorBanner(message = it)
                    Spacer(Modifier.height(16.dp))
                }
                QuestionsStep(uiState, viewModel, modifier = Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                uiState.errorMessage?.let {
                    ErrorBanner(message = it)
                    Spacer(Modifier.height(16.dp))
                }

                when (uiState.step) {
                    CreateQuizStep.DETAILS -> DetailsStep(uiState, viewModel)
                    CreateQuizStep.SETTINGS -> SettingsStep(uiState, viewModel)
                    CreateQuizStep.REVIEW -> ReviewStep(uiState, isEditMode = uiState.isEditMode)
                    CreateQuizStep.QUESTIONS -> Unit
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StepIndicator(currentIndex: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        STEP_LABELS.forEachIndexed { index, _ ->
            val done = index < currentIndex
            val current = index == currentIndex
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (done || current) BrandIndigo else BorderGray),
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Text(
                        "${index + 1}",
                        color = if (current) Color.White else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (index != STEP_LABELS.lastIndex) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(if (done) BrandIndigo else BorderGray)
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .elevatedSurface(shape = RoundedCornerShape(20.dp))
                .padding(16.dp),
            content = content
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SegmentedToggle(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(AppBackground)
            .padding(4.dp)
    ) {
        options.forEach { (label, value) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) BrandIndigo else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (isSelected) Color.White else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DetailsStep(uiState: CreateQuizUiState, viewModel: CreateQuizViewModel) {
    SectionCard(title = "Quiz Info") {
        OutlinedTextField(
            value = uiState.title,
            onValueChange = viewModel::onTitleChange,
            label = { Text("Quiz title") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Description (optional)") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }

    SectionCard(title = "Timing") {
        SegmentedToggle(
            options = listOf("Overall" to "overall", "Per Question" to "per_question"),
            selected = uiState.timeLimitType,
            onSelect = viewModel::onTimeLimitTypeChange
        )
        Spacer(Modifier.height(16.dp))
        if (uiState.timeLimitType == "overall") {
            Text("${uiState.timeLimitMinutes} minutes total", color = TextSecondary, fontSize = 13.sp)
            Slider(
                value = uiState.timeLimitMinutes.toFloat(),
                onValueChange = { viewModel.onTimeLimitMinutesChange(it.toInt()) },
                valueRange = 1f..60f,
                colors = SliderDefaults.colors(thumbColor = BrandIndigo, activeTrackColor = BrandIndigo)
            )
        } else {
            Text("${uiState.timePerQuestionSeconds} seconds per question", color = TextSecondary, fontSize = 13.sp)
            Slider(
                value = uiState.timePerQuestionSeconds.toFloat(),
                onValueChange = { viewModel.onTimePerQuestionChange(it.toInt()) },
                valueRange = 5f..120f,
                colors = SliderDefaults.colors(thumbColor = BrandIndigo, activeTrackColor = BrandIndigo)
            )
        }
    }

    SectionCard(title = "Options") {
        SettingsRow(label = "Shuffle question order per participant") {
            Switch(
                checked = uiState.shuffleQuestions,
                onCheckedChange = viewModel::onShuffleChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandIndigo)
            )
        }
    }
}

@Composable
private fun QuestionsStep(uiState: CreateQuizUiState, viewModel: CreateQuizViewModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${uiState.selectedQuestionIds.size} selected",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            GradientButton(
                text = "New",
                onClick = viewModel::startNewQuestionDraft,
                leadingIcon = Icons.Default.Add,
                modifier = Modifier.width(130.dp)
            )
        }
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = uiState.questionSearchQuery,
            onValueChange = viewModel::onQuestionSearchChange,
            placeholder = { Text("Search questions or tags") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            trailingIcon = {
                if (uiState.questionSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQuestionSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(label = "All", selected = uiState.questionDifficultyFilter == null) {
                    viewModel.onQuestionDifficultyFilterChange(null)
                }
            }
            items(QuestionDifficulty.entries) { difficulty ->
                FilterChip(label = difficulty.name.lowercase().replaceFirstChar { it.uppercase() }, selected = uiState.questionDifficultyFilter == difficulty) {
                    viewModel.onQuestionDifficultyFilterChange(if (uiState.questionDifficultyFilter == difficulty) null else difficulty)
                }
            }
            if (uiState.availableTags.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(BorderGray))
                }
                items(uiState.availableTags) { tag ->
                    FilterChip(label = tag, selected = uiState.questionTagFilter == tag) {
                        viewModel.onQuestionTagFilterChange(if (uiState.questionTagFilter == tag) null else tag)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        val filtered = uiState.filteredQuestionBank
        when {
            uiState.isLoadingQuestionBank -> Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandIndigo)
            }
            uiState.questionBank.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                EmptyState(
                    icon = Icons.Default.Add,
                    title = "No questions yet",
                    subtitle = "Tap New to add your first question to the bank."
                )
            }
            filtered.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                EmptyState(
                    icon = Icons.Default.Search,
                    title = "No matching questions",
                    subtitle = "Try a different search or clear the filters."
                )
            }
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                items(filtered, key = { it.id }) { question ->
                    QuestionSelectRow(
                        question = question,
                        isSelected = question.id in uiState.selectedQuestionIds,
                        onToggle = { viewModel.toggleQuestionSelected(question.id) }
                    )
                }
            }
        }
    }

    uiState.questionDraft?.let {
        NewQuestionSheet(
            draft = it,
            isSaving = uiState.isSavingQuestion,
            onUpdate = viewModel::updateDraft,
            onDismiss = viewModel::cancelNewQuestionDraft,
            onSave = viewModel::saveNewQuestion
        )
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

@Composable
private fun QuestionSelectRow(question: Question, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp, color = if (isSelected) BrandIndigoLight else SurfaceWhite)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectionTick(isSelected = isSelected)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(question.text, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledPill(text = typeLabel(question.type))
                Text("${question.points} pt", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun SelectionTick(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (isSelected) StatGreenIcon else Color.Transparent)
            .border(1.5.dp, if (isSelected) StatGreenIcon else BorderGray, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewQuestionSheet(
    draft: NewQuestionDraft,
    isSaving: Boolean,
    onUpdate: ((NewQuestionDraft) -> NewQuestionDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = SurfaceWhite) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("New Question", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                    Text("Add it to your bank and this quiz", color = TextSecondary, fontSize = 13.sp)
                }
                IconButton(onClick = onDismiss) {
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
                    onClick = { onUpdate { it.copy(type = QuestionType.SINGLE_CHOICE) } }
                )
                QuestionTypeOption(
                    label = "Multiple Choice",
                    subtitle = "Many correct answers",
                    selected = draft.type == QuestionType.MULTI_CHOICE,
                    modifier = Modifier.weight(1f),
                    onClick = { onUpdate { it.copy(type = QuestionType.MULTI_CHOICE) } }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuestionTypeOption(
                    label = "Free Text",
                    subtitle = "Open written response",
                    selected = draft.type == QuestionType.FREE_TEXT,
                    modifier = Modifier.weight(1f),
                    onClick = { onUpdate { it.copy(type = QuestionType.FREE_TEXT) } }
                )
                QuestionTypeOption(
                    label = "Fill in the Blanks",
                    subtitle = "Auto-graded text",
                    selected = draft.type == QuestionType.FILL_IN_BLANK,
                    modifier = Modifier.weight(1f),
                    onClick = { onUpdate { it.copy(type = QuestionType.FILL_IN_BLANK) } }
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("QUESTION TEXT", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.text,
                onValueChange = { text -> onUpdate { it.copy(text = text) } },
                placeholder = { Text("Type your question here...") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(110.dp)
            )

            if (draft.type == QuestionType.SINGLE_CHOICE || draft.type == QuestionType.MULTI_CHOICE) {
                Spacer(Modifier.height(20.dp))
                Text("OPTIONS", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(8.dp))
                draft.options.forEachIndexed { index, optionText ->
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (draft.type == QuestionType.SINGLE_CHOICE) {
                            androidx.compose.material3.RadioButton(
                                selected = draft.correctOptionIndex == index,
                                onClick = { onUpdate { it.copy(correctOptionIndex = index) } }
                            )
                        } else {
                            Checkbox(
                                checked = index in draft.correctOptionIndices,
                                onCheckedChange = { checked ->
                                    onUpdate {
                                        val updated = if (checked) it.correctOptionIndices + index else it.correctOptionIndices - index
                                        it.copy(correctOptionIndices = updated)
                                    }
                                }
                            )
                        }
                        OutlinedTextField(
                            value = optionText,
                            onValueChange = { text ->
                                onUpdate { d -> d.copy(options = d.options.toMutableList().also { it[index] = text }) }
                            },
                            placeholder = { Text("Option ${index + 1}") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                AddRemoveOptionsRow(draft, onUpdate)
            } else {
                Spacer(Modifier.height(16.dp))
                Text("ANSWER", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draft.freeTextAnswer,
                    onValueChange = { text -> onUpdate { it.copy(freeTextAnswer = text) } },
                    placeholder = { Text("Model answer (optional, for your reference)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("Points: ${draft.points}", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Slider(
                value = draft.points.toFloat(),
                onValueChange = { onUpdate { d -> d.copy(points = it.toInt().coerceAtLeast(1)) } },
                valueRange = 1f..10f,
                colors = SliderDefaults.colors(thumbColor = BrandIndigo, activeTrackColor = BrandIndigo)
            )

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, BorderGray, RoundedCornerShape(50))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) { Text("Cancel", color = TextPrimary, fontWeight = FontWeight.Bold) }

                Box(modifier = Modifier.weight(1f)) {
                    GradientButton(text = "Save Question", onClick = onSave, loading = isSaving, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun AddRemoveOptionsRow(draft: NewQuestionDraft, onUpdate: ((NewQuestionDraft) -> NewQuestionDraft) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (draft.options.size < 6) {
            Text(
                "+ Add option",
                color = BrandIndigo,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onUpdate { it.copy(options = it.options + "") } }
            )
        }
        if (draft.options.size > 2) {
            Text(
                "− Remove option",
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    onUpdate {
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
}

private fun typeLabel(type: QuestionType) = when (type) {
    QuestionType.SINGLE_CHOICE -> "Single Choice"
    QuestionType.MULTI_CHOICE -> "Multi Choice"
    QuestionType.FREE_TEXT -> "Free Text"
    QuestionType.FILL_IN_BLANK -> "Fill in Blank"
}

@Composable
private fun SettingsStep(uiState: CreateQuizUiState, viewModel: CreateQuizViewModel) {
    SectionCard(title = "Results") {
        SwitchSettingsRow("Show results after submission", uiState.showResults, viewModel::onShowResultsChange)
        SwitchSettingsRow("Email results to participant", uiState.sendResultEmail, viewModel::onSendResultEmailChange)
        SwitchSettingsRow("Allow downloading result as PDF", uiState.allowResultPdf, viewModel::onAllowResultPdfChange)
        SwitchSettingsRow("Show leaderboard", uiState.showLeaderboard, viewModel::onShowLeaderboardChange, isLast = true)
    }

    SectionCard(title = "Participant Details") {
        SwitchSettingsRow("Collect email", uiState.collectEmail, viewModel::onCollectEmailChange)
        SwitchSettingsRow("Collect phone number", uiState.collectPhone, viewModel::onCollectPhoneChange)
        SwitchSettingsRow("Collect address", uiState.collectAddress, viewModel::onCollectAddressChange)
        SwitchSettingsRow("Show my contact details to participants", uiState.showContactDetails, viewModel::onShowContactDetailsChange)
        SwitchSettingsRow("Require email OTP verification", uiState.requireOtpVerification, viewModel::onRequireOtpChange)
        SwitchSettingsRow("Allow multiple attempts", uiState.allowMultipleAttempts, viewModel::onAllowMultipleAttemptsChange, isLast = true)
    }

    SectionCard(title = "Instructions") {
        OutlinedTextField(
            value = uiState.instructions,
            onValueChange = viewModel::onInstructionsChange,
            placeholder = { Text("Shown to participants before the quiz starts") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }

    SectionCard(title = "Accent Color") {
        Row {
            QUIZ_COLOR_SWATCHES.forEach { hex ->
                val color = Color(android.graphics.Color.parseColor(hex))
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (uiState.quizColor == hex) 3.dp else 0.dp,
                            color = TextPrimary,
                            shape = CircleShape
                        )
                        .clickable { viewModel.onQuizColorChange(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.quizColor == hex) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchSettingsRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, isLast: Boolean = false) {
    Column {
        SettingsRow(label = label) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandIndigo)
            )
        }
        if (!isLast) {
            androidx.compose.material3.HorizontalDivider(color = BorderGray, thickness = 1.dp)
        }
    }
}

@Composable
private fun ReviewStep(uiState: CreateQuizUiState, isEditMode: Boolean) {
    SectionCard(title = if (isEditMode) "Ready to Save" else "Ready to Create") {
        Text(uiState.title, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
        if (uiState.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(uiState.description, color = TextSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))

        ReviewRow("Questions", "${uiState.selectedQuestionIds.size} selected")
        ReviewRow(
            "Timing",
            if (uiState.timeLimitType == "overall") "${uiState.timeLimitMinutes} min overall" else "${uiState.timePerQuestionSeconds}s per question"
        )
        ReviewRow("Leaderboard", if (uiState.showLeaderboard) "Enabled" else "Disabled")
        ReviewRow("OTP verification", if (uiState.requireOtpVerification) "Required" else "Not required")
        ReviewRow("Attempts", if (uiState.allowMultipleAttempts) "Multiple allowed" else "Single attempt only")
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        OutlinedPill(text = value)
    }
}

