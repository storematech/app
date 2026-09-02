package com.quizmaker.android.ui.quizcreate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.quizmaker.android.util.formatPoints
import com.quizmaker.android.data.model.QuestionDifficulty
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.data.model.QuizNameSuggestion
import com.quizmaker.android.ui.aiquiz.MAX_AI_QUESTION_COUNT
import com.quizmaker.android.ui.aiquiz.MIN_AI_QUESTION_COUNT
import com.quizmaker.android.ui.common.BlurBehindDialog
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.FilledPill
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.MathPreview
import com.quizmaker.android.ui.common.MathText
import com.quizmaker.android.ui.common.OutlinedPill
import com.quizmaker.android.ui.common.QuestionTypeOption
import com.quizmaker.android.ui.common.SettingsRow
import com.quizmaker.android.ui.common.TrialPaywallSheet
import com.quizmaker.android.ui.common.elevatedSurface

private val STEP_LABELS = listOf("Details", "Questions", "Settings", "Review")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuizScreen(
    onNavigateBack: () -> Unit,
    onQuizCreated: (String) -> Unit,
    onOpenPricing: () -> Unit = {},
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
            // Scaffold's topBar slot gets no automatic system-bar inset padding on its own (only
            // material3's own TopAppBar composable applies that internally) — since the app runs
            // edge-to-edge, without this the back button rendered partly under the status bar.
            Column(modifier = Modifier.background(AppBackground).statusBarsPadding()) {
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
                val showQuickPublish = uiState.step == CreateQuizStep.DETAILS && viewModel.isQuickPublishAvailable
                if (showQuickPublish) {
                    // Secondary-styled "Next" (same visual weight as the "Back" button elsewhere in
                    // this row) so "Publish Now" reads as the primary action here, mirroring how a
                    // GradientButton is the emphasized choice on every other step.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, BorderGray, RoundedCornerShape(50))
                            .clickable(onClick = viewModel::goNext),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Next", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                GradientButton(
                    text = when {
                        uiState.step == CreateQuizStep.REVIEW -> reviewLabel
                        showQuickPublish -> "Publish Now"
                        else -> "Next"
                    },
                    onClick = {
                        if (uiState.step == CreateQuizStep.REVIEW || showQuickPublish) viewModel.submit() else viewModel.goNext()
                    },
                    enabled = canProceed,
                    loading = uiState.isSubmitting,
                    modifier = Modifier.weight(if (uiState.step == CreateQuizStep.DETAILS && !showQuickPublish) 1f else 1.4f)
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

    if (uiState.showTrialPaywall) {
        TrialPaywallSheet(onDismiss = viewModel::dismissTrialPaywall, onViewPlans = onOpenPricing)
    }

    if (uiState.showAiQuestionSheet) {
        AiQuestionSheet(uiState = uiState, viewModel = viewModel)
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

        // Tap-to-fill starters — only for a brand-new quiz (an edit already has a real title/
        // description worth keeping) — so someone just exploring the app doesn't have to think of
        // a title themselves. See CreateQuizViewModel.reshuffleQuizNameSuggestions.
        if (!uiState.isEditMode && uiState.quizNameSuggestions.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Need ideas? Tap one to fill it in", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::reshuffleQuizNameSuggestions, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Shuffle suggestions", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.quizNameSuggestions, key = { it.title }) { suggestion ->
                    QuizNameSuggestionChip(suggestion = suggestion, onClick = { viewModel.applyQuizNameSuggestion(suggestion) })
                }
            }
        }
    }

    SectionCard(title = "Timing") {
        SegmentedToggle(
            options = listOf("Overall" to "overall", "Per Question" to "per_question"),
            selected = uiState.timeLimitType,
            onSelect = viewModel::onTimeLimitTypeChange
        )
        Spacer(Modifier.height(16.dp))
        if (uiState.timeLimitType == "overall") {
            Text("Total time (minutes, up to 3.5 hours)", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            IntStepper(
                value = uiState.timeLimitMinutes,
                onValueChange = viewModel::onTimeLimitMinutesChange,
                minValue = 1,
                maxValue = MAX_TIME_LIMIT_MINUTES
            )
        } else {
            Text("Seconds per question", color = TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            IntStepper(
                value = uiState.timePerQuestionSeconds,
                onValueChange = viewModel::onTimePerQuestionChange,
                minValue = 5,
                maxValue = 120
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

    SectionCard(title = "Negative Marking") {
        SettingsRow(label = "Enable negative marking") {
            Switch(
                checked = uiState.negativeMarkingMode != "none",
                onCheckedChange = viewModel::onNegativeMarkingEnabledChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandIndigo)
            )
        }
        if (uiState.negativeMarkingMode != "none") {
            Spacer(Modifier.height(14.dp))
            SegmentedToggle(
                options = listOf("Same for all questions" to "uniform", "Set per question" to "per_question"),
                selected = uiState.negativeMarkingMode,
                onSelect = viewModel::onNegativeMarkingModeChange
            )
            Spacer(Modifier.height(14.dp))
            if (uiState.negativeMarkingMode == "uniform") {
                Text("Deduct on every wrong answer", color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                PointsStepper(
                    label = "Negative points",
                    value = uiState.negativeMarkingValue,
                    onValueChange = viewModel::onNegativeMarkingValueChange,
                    minValue = 0.25
                )
            } else {
                InfoAlert("You'll set how many points to deduct for each question individually when adding it.")
            }
        }
    }
}

/** Small "tab"-style chip showing just the suggestion's title (its description fills in
 *  alongside on tap — see CreateQuizViewModel.applyQuizNameSuggestion) — deliberately compact
 *  since several sit side by side in one scrollable row. */
@Composable
private fun QuizNameSuggestionChip(suggestion: QuizNameSuggestion, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(BrandIndigoLight)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            suggestion.title,
            color = BrandIndigo,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/** Light-indigo info banner — same "you need to do something" nudge used for the per-question
 *  negative marking reminder, kept generic enough to reuse elsewhere in this screen if needed. */
@Composable
private fun InfoAlert(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BrandIndigoLight)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(message, color = TextPrimary, fontSize = 13.sp, lineHeight = 18.sp)
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradientButton(
                    text = "AI",
                    onClick = viewModel::openAiQuestionSheet,
                    leadingIcon = Icons.Default.AutoAwesome,
                    height = 40.dp,
                    modifier = Modifier.width(84.dp)
                )
                GradientButton(
                    text = "New",
                    onClick = viewModel::startNewQuestionDraft,
                    leadingIcon = Icons.Default.Add,
                    height = 40.dp,
                    modifier = Modifier.width(104.dp)
                )
            }
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
            negativeMarkingMode = uiState.negativeMarkingMode,
            isSaving = uiState.isSavingQuestion,
            onUpdate = viewModel::updateDraft,
            onDismiss = viewModel::cancelNewQuestionDraft,
            onSave = viewModel::saveNewQuestion
        )
    }
}

/**
 * Questions step's "AI" button — generates straight into this wizard's own question bank/
 * selection (see CreateQuizViewModel.generateAiQuestions), no separate review step, no leaving
 * this screen. Same ModalBottomSheet pattern as NewQuestionSheet just above.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiQuestionSheet(uiState: CreateQuizUiState, viewModel: CreateQuizViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = viewModel::dismissAiQuestionSheet, sheetState = sheetState, containerColor = SurfaceWhite) {
        BlurBehindDialog()
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Add questions with AI", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                    Text(
                        if (uiState.title.isNotBlank()) "For \"${uiState.title}\"" else "Straight into this quiz's question list",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = viewModel::dismissAiQuestionSheet) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(16.dp))

            uiState.aiQuestionError?.let {
                ErrorBanner(message = it)
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = uiState.aiPrompt,
                onValueChange = viewModel::onAiPromptChange,
                label = { Text("Topic") },
                placeholder = { Text("e.g. Photosynthesis, chapter 4") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                enabled = !uiState.isGeneratingAiQuestions,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Questions", color = TextSecondary, fontSize = 13.sp)
                IntStepper(
                    value = uiState.aiQuestionCount,
                    onValueChange = viewModel::onAiQuestionCountChange,
                    minValue = MIN_AI_QUESTION_COUNT,
                    maxValue = MAX_AI_QUESTION_COUNT
                )
            }
            Spacer(Modifier.height(20.dp))

            GradientButton(
                text = "Generate",
                onClick = viewModel::generateAiQuestions,
                leadingIcon = Icons.Default.AutoAwesome,
                enabled = uiState.aiPrompt.isNotBlank() && !uiState.isGeneratingAiQuestions,
                loading = uiState.isGeneratingAiQuestions,
                modifier = Modifier.fillMaxWidth()
            )
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
            MathText(text = question.text, color = TextPrimary, fontSize = 14.sp, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledPill(text = typeLabel(question.type))
                Text("${question.points.formatPoints()} pt", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
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
    negativeMarkingMode: String,
    isSaving: Boolean,
    onUpdate: ((NewQuestionDraft) -> NewQuestionDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = SurfaceWhite) {
        BlurBehindDialog()
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
                placeholder = { Text("Type your question here... (supports LaTeX math, e.g. $\\frac{a}{b}$)") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(110.dp)
            )
            MathPreview(draft.text)

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
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = optionText,
                                onValueChange = { text ->
                                    onUpdate { d -> d.copy(options = d.options.toMutableList().also { it[index] = text }) }
                                },
                                placeholder = { Text("Option ${index + 1}") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            MathPreview(optionText)
                        }
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
            Text("DIFFICULTY", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuestionDifficulty.entries.forEach { difficulty ->
                    FilterChip(
                        label = difficulty.value.replaceFirstChar { c -> c.uppercase() },
                        selected = draft.difficulty == difficulty
                    ) {
                        onUpdate { it.copy(difficulty = difficulty) }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Points", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            PointsStepper(
                label = "Points",
                value = draft.points,
                onValueChange = { onUpdate { d -> d.copy(points = it) } }
            )
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onUpdate { d -> d.copy(isUngraded = !d.isUngraded) } }
            ) {
                Checkbox(
                    checked = draft.isUngraded,
                    onCheckedChange = { checked -> onUpdate { d -> d.copy(isUngraded = checked) } }
                )
                Spacer(Modifier.width(4.dp))
                Column {
                    Text("Ungraded", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    if (draft.type == QuestionType.FREE_TEXT) {
                        Text(
                            "Unchecked = you'll manually award points after each submission",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Enable Negative Marking",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = draft.negativePoints > 0,
                    onCheckedChange = { enabled ->
                        onUpdate { d -> d.copy(negativePoints = if (enabled) d.points else 0.0) }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandIndigo)
                )
            }
            // The quiz is set to "Set per question" — nudge that this specific question still
            // needs its own value, since there's no quiz-wide default to fall back on here.
            if (negativeMarkingMode == "per_question" && draft.negativePoints <= 0) {
                Spacer(Modifier.height(10.dp))
                InfoAlert("This quiz uses per-question negative marking — set how many points to deduct for this question.")
            }
            if (draft.negativePoints > 0) {
                Spacer(Modifier.height(8.dp))
                Text("Deduct on wrong answer", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                PointsStepper(
                    label = "Negative points",
                    value = draft.negativePoints,
                    onValueChange = { onUpdate { d -> d.copy(negativePoints = it) } },
                    minValue = 0.25
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("TAGS", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            var tagsText by remember { mutableStateOf(draft.tags.joinToString(", ")) }
            OutlinedTextField(
                value = tagsText,
                onValueChange = { text ->
                    tagsText = text
                    onUpdate { it.copy(tags = text.split(",").map { tag -> tag.trim() }.filter { tag -> tag.isNotBlank() }) }
                },
                placeholder = { Text("e.g. algebra, geometry") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            Text("EXPLANATION (OPTIONAL)", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.explanation.orEmpty(),
                onValueChange = { text -> onUpdate { it.copy(explanation = text.ifBlank { null }) } },
                placeholder = { Text("Explain why this answer is correct...") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(90.dp)
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

/** +/- always moves by exactly 1 regardless of the current decimal value; the field in the middle
 *  can also be typed into directly, including decimals like "1.2". Mirrors QuestionEditSheet.kt's
 *  own copy — this screen already keeps its own duplicate NewQuestionDraft/sheet, so matching that
 *  existing duplication is more consistent here than a one-off shared extraction. */
@Composable
private fun PointsStepper(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    minValue: Double = 0.25,
    maxValue: Double = 100.0
) {
    var text by remember(value) { mutableStateOf(value.formatPoints()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onValueChange((value - 1.0).coerceIn(minValue, maxValue)) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease $label", tint = BrandIndigo)
        }
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input
                input.toDoubleOrNull()?.let { onValueChange(it.coerceIn(minValue, maxValue)) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(84.dp)
        )
        IconButton(
            onClick = { onValueChange((value + 1.0).coerceIn(minValue, maxValue)) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase $label", tint = BrandIndigo)
        }
    }
}

/** Same +/- + type-directly shape as [PointsStepper], for whole-number values (quiz timing) rather
 *  than decimals — kept separate since the two need different keyboard types/parsing. */
@Composable
private fun IntStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int,
    maxValue: Int
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onValueChange((value - 1).coerceIn(minValue, maxValue)) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = BrandIndigo)
        }
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input
                input.toIntOrNull()?.let { onValueChange(it.coerceIn(minValue, maxValue)) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(84.dp)
        )
        IconButton(
            onClick = { onValueChange((value + 1).coerceIn(minValue, maxValue)) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase", tint = BrandIndigo)
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
    var showColorPicker by remember { mutableStateOf(false) }

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
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            QUIZ_COLOR_SWATCHES.forEach { hex ->
                val color = Color(android.graphics.Color.parseColor(hex))
                val isSelected = uiState.quizColor == hex
                // A halo ring in the swatch's own color, offset from the swatch itself, reads as
                // "selected" more clearly at a glance than a border drawn directly on the color —
                // and the 48dp outer box is a proper touch target regardless of the swatch's own size.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(width = if (isSelected) 2.dp else 0.dp, color = color, shape = CircleShape)
                        .clickable { viewModel.onQuizColorChange(hex) }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // A 6th "custom" swatch — same halo-ring treatment as the presets. Shows the actually
            // picked custom color (with its own check mark) once one is active; otherwise a
            // rainbow ring around a "+" is what invites tapping it in the first place.
            val isCustomActive = uiState.quizColor !in QUIZ_COLOR_SWATCHES
            val customColor = if (isCustomActive) runCatching { Color(android.graphics.Color.parseColor(uiState.quizColor)) }.getOrNull() else null
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        brush = if (customColor != null) Brush.horizontalGradient(listOf(customColor, customColor)) else Brush.sweepGradient(RAINBOW_HUES),
                        shape = CircleShape
                    )
                    .clickable { showColorPicker = true }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (customColor != null) {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(customColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Custom color selected", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(Brush.sweepGradient(RAINBOW_HUES)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Pick a custom color", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initialHex = uiState.quizColor,
            onConfirm = { hex ->
                viewModel.onQuizColorChange(hex)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

/** Full-spectrum stops for the "pick a custom color" ring/swatch and the hue slider below — red
 *  back to red so a [Brush.sweepGradient]/[Brush.horizontalGradient] built from it wraps cleanly. */
private val RAINBOW_HUES = listOf(
    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
)

/**
 * A dependency-free HSV color picker: a saturation/brightness square (drag or tap anywhere) plus a
 * hue bar below it, with a hex field wired both ways so precise values can be typed directly too.
 * No color-picker library exists in this project yet, and this is the only place one's needed.
 */
@Composable
private fun ColorPickerDialog(initialHex: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val initialHsv = remember {
        val argb = runCatching { android.graphics.Color.parseColor(initialHex) }
            .getOrDefault(android.graphics.Color.parseColor(QUIZ_COLOR_SWATCHES.first()))
        FloatArray(3).also { android.graphics.Color.colorToHSV(argb, it) }
    }
    var hue by remember { mutableStateOf(initialHsv[0]) }
    var saturation by remember { mutableStateOf(initialHsv[1]) }
    var brightness by remember { mutableStateOf(initialHsv[2]) }

    val selectedColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
    val selectedHex = "#%06X".format(selectedColor.toArgb() and 0xFFFFFF)

    // Only follows the picker's own hue/sat/brightness changes — typing into the field updates
    // hue/sat/brightness directly (see its onValueChange below) rather than the other way round,
    // so an in-progress, not-yet-valid hex string typed by the user is never stomped mid-edit.
    var hexInput by remember { mutableStateOf(selectedHex) }
    LaunchedEffect(selectedHex) { hexInput = selectedHex }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            BlurBehindDialog()
            Text("Custom Color", color = TextPrimary, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(1.dp, BorderGray, CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            hexInput = input
                            val normalized = if (input.startsWith("#")) input else "#$input"
                            if (Regex("^#[0-9A-Fa-f]{6}$").matches(normalized)) {
                                runCatching { android.graphics.Color.parseColor(normalized) }.getOrNull()?.let { argb ->
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(argb, hsv)
                                    hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2]
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(18.dp))
                SaturationBrightnessBox(
                    hue = hue,
                    saturation = saturation,
                    brightness = brightness,
                    onChange = { s, b -> saturation = s; brightness = b }
                )
                Spacer(Modifier.height(14.dp))
                HueSliderBar(hue = hue, onHueChange = { hue = it })
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = { onConfirm(selectedHex) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = BrandIndigo)
            ) {
                Text("Use Color", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun SaturationBrightnessBox(hue: Float, saturation: Float, brightness: Float, onChange: (Float, Float) -> Unit) {
    val hueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
    var boxSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(hueColor)
            .background(Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                fun updateFrom(offset: androidx.compose.ui.geometry.Offset) {
                    if (boxSize.width == 0 || boxSize.height == 0) return
                    val s = (offset.x / boxSize.width).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / boxSize.height).coerceIn(0f, 1f)
                    onChange(s, v)
                }
                detectDragGestures(
                    onDragStart = { offset -> updateFrom(offset) },
                    onDrag = { change, _ -> change.consume(); updateFrom(change.position) }
                )
            }
    ) {
        if (boxSize.width > 0 && boxSize.height > 0) {
            val thumbX = with(density) { (saturation * boxSize.width).toDp() } - 10.dp
            val thumbY = with(density) { ((1f - brightness) * boxSize.height).toDp() } - 10.dp
            Box(
                modifier = Modifier
                    .offset(x = thumbX, y = thumbY)
                    .size(20.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .border(1.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
            )
        }
    }
}

@Composable
private fun HueSliderBar(hue: Float, onHueChange: (Float) -> Unit) {
    var barSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(RAINBOW_HUES))
            .onSizeChanged { barSize = it }
            .pointerInput(Unit) {
                fun updateFrom(x: Float) {
                    if (barSize.width == 0) return
                    onHueChange(((x / barSize.width).coerceIn(0f, 1f) * 360f).coerceAtMost(359.99f))
                }
                detectDragGestures(
                    onDragStart = { offset -> updateFrom(offset.x) },
                    onDrag = { change, _ -> change.consume(); updateFrom(change.position.x) }
                )
            }
    ) {
        if (barSize.width > 0) {
            val thumbX = with(density) { ((hue / 360f) * barSize.width).toDp() } - 4.dp
            Box(
                modifier = Modifier
                    .offset(x = thumbX)
                    .width(8.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .border(1.dp, Color.Black.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            )
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

