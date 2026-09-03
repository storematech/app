package com.quizmaker.android.ui.fulltest

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.StatGreenIcon
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.ExamCategory
import com.quizmaker.android.data.model.ExamSuggestion
import com.quizmaker.android.data.model.FullTestChapterConfig
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionFormat
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.MathText
import com.quizmaker.android.ui.common.TrialPaywallSheet
import com.quizmaker.android.ui.common.elevatedSurface

/**
 * "Full Test" mode — the AI Quiz screen's second entry point alongside the existing prompt-based
 * Quick Test. A 5-step wizard (see FullTestStep): pick/name a real exam, the AI researches its
 * actual structure, the user reviews/edits the generated configuration, the AI generates the full
 * test batched per chapter, then a final review/select step identical in spirit to Quick Test's —
 * confirming hands the saved question ids off to Create Quiz, same as everywhere else in the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullTestScreen(
    onNavigateToCreateQuiz: (List<String>, String?) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenPricing: () -> Unit,
    onNavigateToQuizList: () -> Unit,
    onOpenQuiz: (String) -> Unit,
    viewModel: FullTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.navigateToCreateQuizWith) {
        uiState.navigateToCreateQuizWith?.let { ids ->
            val title = uiState.navigateToCreateQuizTitle
            viewModel.consumeNavigation()
            onNavigateToCreateQuiz(ids, title)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Soft 4-corner gradient aura behind the whole wizard — a premium, "AI product" backdrop
        // instead of a flat white screen, consistent across every step rather than flickering in
        // only on this one. The Scaffold itself stays transparent so this shows through everywhere.
        PremiumAuraBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // A real TopAppBar composable pads itself below the status bar automatically —
                // this hand-rolled Row doesn't, so without statusBarsPadding() here the back
                // button chip renders half-hidden behind the status bar instead of clear of it.
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // A plain icon here would wash out against the corner blobs (that's exactly
                    // where the top-left one sits) — a frosted backdrop chip keeps it visible no
                    // matter what color is behind it. The chip is a separate Box BEHIND the
                    // IconButton (not size/clip/background applied directly to IconButton itself)
                    // — IconButton enforces its own internal fixed size + clip + state-layer, and
                    // stacking a smaller explicit size/clip on top of that clashed with it,
                    // clipping part of the icon/circle off.
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceWhite.copy(alpha = 0.9f))
                        )
                        IconButton(
                            onClick = {
                                when (uiState.step) {
                                    FullTestStep.SELECT_EXAM -> onNavigateBack()
                                    FullTestStep.CONFIGURE -> viewModel.onBackToSelectExam()
                                    else -> onNavigateBack()
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when (uiState.step) {
                            FullTestStep.SELECT_EXAM -> "Full Test"
                            FullTestStep.RESEARCHING -> "Researching…"
                            FullTestStep.CONFIGURE -> "Configure Test"
                            FullTestStep.GENERATING -> "Generating…"
                            FullTestStep.REVIEW -> "Review Questions"
                        },
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                }
            },
            bottomBar = {
                // Fixed at the bottom instead of living at the end of SelectExamStep's scrollable
                // content — previously reaching "Create Full Test" meant scrolling past every
                // suggestion row first, on every visit to this step.
                if (uiState.step == FullTestStep.SELECT_EXAM) {
                    Surface(color = SurfaceWhite, shadowElevation = 8.dp) {
                        Box(modifier = Modifier.navigationBarsPadding().padding(20.dp)) {
                            GradientButton(
                                text = "Create Full Test",
                                onClick = viewModel::startResearch,
                                enabled = uiState.examName.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (uiState.step) {
                    FullTestStep.SELECT_EXAM -> SelectExamStep(
                        examName = uiState.examName,
                        errorMessage = uiState.errorMessage,
                        recentQuizzes = uiState.recentQuizzes,
                        recentQuizQuestionCounts = uiState.recentQuizQuestionCounts,
                        onOpenQuiz = onOpenQuiz,
                        onViewAllQuizzes = onNavigateToQuizList,
                        viewModel = viewModel
                    )
                    FullTestStep.RESEARCHING -> LoadingStep(
                        title = "Researching ${uiState.examName}",
                        subtitle = "Looking up the real exam pattern — question count, marks, chapters, and difficulty."
                    )
                    FullTestStep.CONFIGURE -> ConfigureStep(uiState, viewModel)
                    FullTestStep.GENERATING -> LoadingStep(
                        title = "Generating your full test",
                        subtitle = "This can take a minute or two for a large test — please keep this screen open."
                    )
                    FullTestStep.REVIEW -> ReviewStep(uiState, viewModel)
                }
            }
        }
    }

    if (uiState.showTrialPaywall) {
        TrialPaywallSheet(onDismiss = viewModel::dismissTrialPaywall, onViewPlans = onOpenPricing)
    }
}

/** Four large, softly blurred radial-gradient blobs — one per corner — fading into
 *  [AppBackground], in the same blue/violet/pink/orange palette as the Quick|Full Test tab's own
 *  "Gemini style" transition, so the whole Full Test flow reads as one consistent premium AI
 *  surface rather than a flat white wizard. Deliberately low alpha + blur so body text and cards
 *  everywhere else stay fully legible on top of it. */
@Composable
private fun PremiumAuraBackground() {
    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        AuraBlob(color = Color(0xFF4C8DF6), modifier = Modifier.align(Alignment.TopStart).offset(x = (-70).dp, y = (-70).dp))
        AuraBlob(color = Color(0xFF9C6ADE), modifier = Modifier.align(Alignment.TopEnd).offset(x = 70.dp, y = (-70).dp))
        AuraBlob(color = Color(0xFFF76CA6), modifier = Modifier.align(Alignment.BottomStart).offset(x = (-70).dp, y = 70.dp))
        AuraBlob(color = Color(0xFFFDB750), modifier = Modifier.align(Alignment.BottomEnd).offset(x = 70.dp, y = 70.dp))
    }
}

@Composable
private fun AuraBlob(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(240.dp)
            .blur(90.dp)
            .background(
                Brush.radialGradient(colors = listOf(color.copy(alpha = 0.4f), Color.Transparent)),
                shape = CircleShape
            )
    )
}

@Composable
private fun LoadingStep(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = BrandIndigo)
        Spacer(Modifier.height(20.dp))
        Text(title, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
    }
}

@Composable
private fun SelectExamStep(
    examName: String,
    errorMessage: String?,
    recentQuizzes: List<Quiz>,
    recentQuizQuestionCounts: Map<String, Int>,
    onOpenQuiz: (String) -> Unit,
    onViewAllQuizzes: () -> Unit,
    viewModel: FullTestViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp)
    ) {
        Text("Which exam?", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text("Type an exam name, or tap a suggestion below", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))

        errorMessage?.let {
            ErrorBanner(message = it)
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = examName,
            onValueChange = viewModel::onExamNameChange,
            placeholder = { Text("Search for an exam…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            trailingIcon = {
                if (examName.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onExamNameChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = BrandIndigo,
                unfocusedContainerColor = SurfaceWhite,
                focusedContainerColor = SurfaceWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        if (recentQuizzes.isNotEmpty()) {
            RecentQuizSection(
                quizzes = recentQuizzes,
                questionCounts = recentQuizQuestionCounts,
                onOpenQuiz = onOpenQuiz,
                onViewAll = onViewAllQuizzes
            )
            Spacer(Modifier.height(24.dp))
        }

        ExamSuggestionRow("Exam Prep", ExamCategory.EXAM_PREP, viewModel)
        Spacer(Modifier.height(20.dp))
        ExamSuggestionRow("Job Prep", ExamCategory.JOB_PREP, viewModel)
        Spacer(Modifier.height(20.dp))
        ExamSuggestionRow("Schools", ExamCategory.SCHOOLS, viewModel)
    }
}

/** Up to RECENT_QUIZZES_LIMIT of the user's most recently created quizzes, shown right below the
 *  search box — "View all" hands off to the same Quiz List screen the bottom nav tab opens. */
@Composable
private fun RecentQuizSection(
    quizzes: List<Quiz>,
    questionCounts: Map<String, Int>,
    onOpenQuiz: (String) -> Unit,
    onViewAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Recent Quiz", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
        TextButton(onClick = onViewAll) {
            Text("View all", color = BrandIndigo, fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
    Spacer(Modifier.height(4.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(quizzes, key = { it.id }) { quiz ->
            RecentQuizCard(
                title = quiz.title,
                questionCount = questionCounts[quiz.id],
                onClick = { onOpenQuiz(quiz.id) }
            )
        }
    }
}

@Composable
private fun RecentQuizCard(title: String, questionCount: Int?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .height(108.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(BrandIndigo.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (questionCount != null) {
            Spacer(Modifier.height(2.dp))
            Text("$questionCount Qs", color = TextSecondary, fontSize = 10.sp)
        }
    }
}

/** Two visible rows (LazyHorizontalGrid, scrollable sideways) instead of the single long
 *  horizontal-scroll row this used to be — "View all" opens a scrollable popup with the rest of
 *  the category instead of making the user scroll an ever-longer single row. */
@Composable
private fun ExamSuggestionRow(label: String, category: ExamCategory, viewModel: FullTestViewModel) {
    var showAllDialog by remember { mutableStateOf(false) }
    val suggestions = viewModel.examSuggestions.filter { it.category == category }
    val theme = themeForExamCategory(category)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
        TextButton(onClick = { showAllDialog = true }) {
            Text("View all", color = BrandIndigo, fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
    Spacer(Modifier.height(4.dp))
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().height(186.dp)
    ) {
        items(suggestions, key = { it.name }) { suggestion ->
            ExamSuggestionCard(
                name = suggestion.name,
                theme = theme,
                onClick = { viewModel.selectExamSuggestion(suggestion.name) }
            )
        }
    }

    if (showAllDialog) {
        ExamSuggestionAllDialog(
            label = label,
            suggestions = suggestions,
            theme = theme,
            onSelect = { name ->
                viewModel.selectExamSuggestion(name)
                showAllDialog = false
            },
            onDismiss = { showAllDialog = false }
        )
    }
}

@Composable
private fun ExamSuggestionAllDialog(
    label: String,
    suggestions: List<ExamSuggestion>,
    theme: ExamCategoryTheme,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSuggestions = remember(suggestions, searchQuery) {
        if (searchQuery.isBlank()) suggestions
        else suggestions.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text(
                    label,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = theme.start,
                        unfocusedContainerColor = AppBackground,
                        focusedContainerColor = AppBackground
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
                if (filteredSuggestions.isEmpty()) {
                    Text(
                        "No matches found",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }
                LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
                    items(filteredSuggestions, key = { it.name }) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(suggestion.name) }
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(theme.start.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(theme.icon, contentDescription = null, tint = theme.start, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(suggestion.name, fontFamily = PoppinsFamily, fontSize = 14.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

/** Same "colored gradient card, watermark icon, bold white label" recipe as AiQuizScreen's own
 *  Trending Templates carousel — matches the visual language of the app's other AI suggestion
 *  chips instead of the plain pill style this row used to use. */
private data class ExamCategoryTheme(val icon: ImageVector, val start: Color, val end: Color)

private fun themeForExamCategory(category: ExamCategory): ExamCategoryTheme = when (category) {
    ExamCategory.EXAM_PREP -> ExamCategoryTheme(Icons.Default.School, Color(0xFF2563EB), Color(0xFF1D4ED8))
    ExamCategory.JOB_PREP -> ExamCategoryTheme(Icons.Default.Work, Color(0xFF0D9488), Color(0xFF115E59))
    ExamCategory.SCHOOLS -> ExamCategoryTheme(Icons.Default.Book, Color(0xFFF59E0B), Color(0xFFB45309))
}

@Composable
private fun ExamSuggestionCard(name: String, theme: ExamCategoryTheme, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .height(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(theme.start, theme.end)))
            .clickable(onClick = onClick)
    ) {
        Icon(
            theme.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.16f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 14.dp, y = 14.dp)
                .size(56.dp)
                .rotate(-15f)
        )
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(theme.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                name,
                color = Color.White,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ConfigureStep(uiState: FullTestUiState, viewModel: FullTestViewModel) {
    val pattern = uiState.examPattern
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        uiState.errorMessage?.let {
            ErrorBanner(message = it)
            Spacer(Modifier.height(16.dp))
        }

        if (pattern != null) {
            Text(pattern.examName, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "${pattern.totalMarks} marks · ${pattern.durationMinutes} min · usually ${pattern.totalQuestions} questions",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(20.dp))
        }

        SectionLabel("Total Questions")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            CountStepper(
                value = uiState.totalQuestions,
                onValueChange = viewModel::onTotalQuestionsChange,
                minValue = 1,
                maxValue = 250
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Reset evenly",
                color = BrandIndigo,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = viewModel::resetEvenly)
            )
        }
        Spacer(Modifier.height(20.dp))

        SectionLabel("Question Format")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuestionFormat.entries.forEach { format ->
                FormatChip(
                    label = format.label,
                    selected = format in uiState.selectedFormats,
                    onClick = { viewModel.onFormatToggle(format) }
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        uiState.negativeMarking?.let { negMarking ->
            SectionLabel("Negative Marking")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().elevatedSurface(shape = RoundedCornerShape(14.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("This exam deducts marks for wrong answers", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("+${negMarking.correctMarks} correct · −${negMarking.incorrectMarks} wrong", color = TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = uiState.negativeMarkingEnabled,
                    onCheckedChange = viewModel::onNegativeMarkingToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandIndigo)
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        SectionLabel("Chapters")
        Spacer(Modifier.height(8.dp))
        uiState.chapters.forEach { chapter ->
            ChapterRow(chapter = chapter, onCountChange = { viewModel.onChapterCountChange(chapter.id, it) })
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(12.dp))

        GradientButton(
            text = "Generate Full Test",
            onClick = viewModel::generateFullTest,
            enabled = uiState.canGenerate,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
}

@Composable
private fun FormatChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) BrandIndigo else AppBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(label, color = if (selected) Color.White else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ChapterRow(chapter: FullTestChapterConfig, onCountChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().elevatedSurface(shape = RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(chapter.chapter, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(chapter.subject, color = TextSecondary, fontSize = 11.sp)
        }
        CountStepper(value = chapter.questionCount, onValueChange = onCountChange, minValue = 0, maxValue = 30)
    }
}

@Composable
private fun CountStepper(value: Int, onValueChange: (Int) -> Unit, minValue: Int, maxValue: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onValueChange((value - 1).coerceIn(minValue, maxValue)) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = BrandIndigo, modifier = Modifier.size(18.dp))
        }
        Text(value.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onValueChange((value + 1).coerceIn(minValue, maxValue)) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Increase", tint = BrandIndigo, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ReviewStep(uiState: FullTestUiState, viewModel: FullTestViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        uiState.errorMessage?.let {
            ErrorBanner(message = it)
            Spacer(Modifier.height(12.dp))
        }
        if (uiState.failedChapters.isNotEmpty()) {
            ErrorBanner(message = "Couldn't generate: ${uiState.failedChapters.joinToString(", ")}. The rest of the test is ready below.")
            Spacer(Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text("${uiState.selectedReviewIds.size} of ${uiState.reviewQuestions.size} selected", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Text(
                if (uiState.selectedReviewIds.size == uiState.reviewQuestions.size) "Clear all" else "Select all",
                color = BrandIndigo,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    if (uiState.selectedReviewIds.size == uiState.reviewQuestions.size) viewModel.deselectAllReview() else viewModel.selectAllReview()
                }
            )
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(uiState.reviewQuestions, key = { it.id }) { question ->
                ReviewQuestionRow(
                    question = question,
                    isSelected = question.id in uiState.selectedReviewIds,
                    onToggle = { viewModel.toggleReviewQuestion(question.id) }
                )
            }
        }

        GradientButton(
            text = "Create Quiz (${uiState.selectedReviewIds.size})",
            onClick = viewModel::confirmSelection,
            enabled = uiState.selectedReviewIds.isNotEmpty() && !uiState.isSaving,
            loading = uiState.isSaving,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ReviewQuestionRow(question: Question, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceWhite)
            .clickable(onClick = onToggle)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) StatGreenIcon else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            else Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BorderGray, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            MathText(text = question.text, color = TextPrimary, fontSize = 13.sp, maxLines = 3)
            Spacer(Modifier.height(4.dp))
            Text(
                question.tags.drop(1).joinToString(" · ").ifBlank { question.difficulty?.value.orEmpty() },
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
