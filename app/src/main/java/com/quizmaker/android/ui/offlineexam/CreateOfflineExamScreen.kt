package com.quizmaker.android.ui.offlineexam

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.quizmaker.android.core.theme.AiCardBg
import com.quizmaker.android.core.theme.AiCardBorder
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.StatPurpleBg
import com.quizmaker.android.core.theme.StatPurpleIcon
import com.quizmaker.android.core.theme.StatGreenIcon
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.QuestionDifficulty
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.ui.aiquiz.MAX_AI_QUESTION_COUNT
import com.quizmaker.android.ui.aiquiz.MIN_AI_QUESTION_COUNT
import com.quizmaker.android.ui.common.BlurBehindDialog
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.FilledPill
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.MathPreview
import com.quizmaker.android.ui.common.MathText
import com.quizmaker.android.ui.common.QuestionTypeOption
import com.quizmaker.android.ui.common.TrialPaywallSheet
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.AiAttachmentUtils
import com.quizmaker.android.util.formatPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single-screen create/edit for an Offline Exam — a normal `quizzes` row flagged
 * `is_offline_exam = true` (see CreateOfflineExamViewModel/QuizRepository.createOfflineExam).
 * Unlike CreateQuizScreen's 4-step wizard, this only ever collects a title/description/question
 * selection: timing, results, participant-detail settings etc. are all fixed defaults the
 * repository bakes in (see QuizRepository.offlineExamSpec), since there's no participant-facing
 * link for any of that to matter for a paper that's printed and handed out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOfflineExamScreen(
    onNavigateBack: () -> Unit,
    onOpenPricing: () -> Unit,
    onOpenPaperPreview: (String) -> Unit,
    viewModel: CreateOfflineExamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    LaunchedEffect(uiState.paperPreviewQuizId) {
        uiState.paperPreviewQuizId?.let { quizId ->
            onOpenPaperPreview(quizId)
            viewModel.consumePaperPreviewNavigation()
        }
    }

    // ---- Scanner (photo) capture state — mirrors AiQuizScreen's capturedUris/camera plumbing ----
    val capturedUris = remember { mutableStateListOf<Uri>() }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var scannerLocalError by remember { mutableStateOf<String?>(null) }
    var isPreparingPhotos by remember { mutableStateOf(false) }
    var showCameraRationale by remember { mutableStateOf(false) }

    // The sheet closes itself (ViewModel-driven) once photos are successfully generated/saved —
    // clear the locally-held capture state at the same time so re-opening it starts fresh.
    LaunchedEffect(uiState.showScannerSheet) {
        if (!uiState.showScannerSheet) {
            capturedUris.clear()
            scannerLocalError = null
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCaptureUri
        if (success && uri != null) {
            capturedUris.add(uri)
            scannerLocalError = null
        }
    }

    fun startCapture() {
        val uri = AiAttachmentUtils.createCaptureUri(context)
        pendingCaptureUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCapture() else scannerLocalError = "Camera permission is needed to take photos."
    }

    fun onAddPhotoTapped() {
        scannerLocalError = null
        if (capturedUris.size >= AiAttachmentUtils.MAX_IMAGES) {
            scannerLocalError = "You can add up to ${AiAttachmentUtils.MAX_IMAGES} photos."
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCapture()
        } else {
            showCameraRationale = true
        }
    }

    fun onGeneratePhotosTapped() {
        if (capturedUris.isEmpty()) {
            scannerLocalError = "Take at least one photo first."
            return
        }
        val urisSnapshot = capturedUris.toList()
        scope.launch {
            isPreparingPhotos = true
            val encoded = mutableListOf<Pair<String, String>>()
            var failure: String? = null
            for (uri in urisSnapshot) {
                when (val result = withContext(Dispatchers.IO) { AiAttachmentUtils.compressImageAsBase64(context, uri) }) {
                    is AiAttachmentUtils.AttachmentResult.Success -> encoded.add(result.base64 to "image/jpeg")
                    is AiAttachmentUtils.AttachmentResult.Error -> {
                        failure = result.message
                        break
                    }
                }
            }
            isPreparingPhotos = false
            if (failure != null) scannerLocalError = failure else viewModel.generateFromPhotos(encoded)
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) "Edit Offline Exam" else "Create Offline Exam",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isEditMode) {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("Delete exam", color = ErrorRed) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                                    onClick = {
                                        menuExpanded = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                GradientButton(
                    text = if (uiState.isEditMode) "Preview Paper" else "Create Exam",
                    onClick = {
                        if (uiState.isEditMode) viewModel.openPaperPreview() else viewModel.createExam()
                    },
                    loading = uiState.isDownloading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        if (uiState.isLoadingForEdit) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandIndigo)
            }
            return@Scaffold
        }

        // The whole screen is one LazyColumn — title/description/buttons/search are a single
        // header `item`, the question rows are lazy `items` below it, so everything scrolls
        // together as one continuous page (previously the header lived in its own small
        // verticalScroll Column while the question list scrolled separately via its own
        // Modifier.weight(1f), which read as "the screen doesn't scroll" whenever the header
        // plus the fixed button/search rows took up most/all of the visible height). Kept the
        // list itself lazy (not folded into a plain Column) for the same reason
        // CreateQuizScreen.QuestionsStep does — a question bank can run into the hundreds and a
        // non-virtualized list visibly froze the UI there.
        val filtered = uiState.filteredQuestionBank
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column {
                    uiState.errorMessage?.let {
                        ErrorBanner(message = it)
                        Spacer(Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = viewModel::onTitleChange,
                        label = { Text("Exam title") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text("Description (optional)") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Questions", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        FilledPill(text = "${uiState.selectedQuestionIds.size} selected")
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        GradientButton(
                            text = "AI",
                            onClick = viewModel::openAiQuestionSheet,
                            leadingIcon = Icons.Default.AutoAwesome,
                            height = 40.dp,
                            modifier = Modifier.weight(1f)
                        )
                        GradientButton(
                            text = "Scanner",
                            onClick = viewModel::openScannerSheet,
                            leadingIcon = Icons.Default.CameraAlt,
                            height = 40.dp,
                            modifier = Modifier.weight(1f)
                        )
                        GradientButton(
                            text = "New",
                            onClick = viewModel::startNewQuestionDraft,
                            leadingIcon = Icons.Default.Add,
                            height = 40.dp,
                            modifier = Modifier.weight(1f)
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
                }
            }

            when {
                uiState.isLoadingQuestionBank -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandIndigo)
                    }
                }
                uiState.questionBank.isEmpty() -> item {
                    EmptyState(
                        icon = Icons.Default.Add,
                        title = "No questions yet",
                        subtitle = "Tap New, AI, or Scanner to add your first question."
                    )
                }
                filtered.isEmpty() -> item {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = "No matching questions",
                        subtitle = "Try a different search."
                    )
                }
                else -> items(filtered, key = { it.id }) { question ->
                    OfflineExamQuestionRow(
                        question = question,
                        isSelected = question.id in uiState.selectedQuestionIds,
                        onToggle = { viewModel.toggleQuestionSelected(question.id) }
                    )
                }
            }
        }
    }

    uiState.questionDraft?.let { draft ->
        NewOfflineExamQuestionSheet(
            draft = draft,
            isSaving = uiState.isSavingQuestion,
            onUpdate = viewModel::updateDraft,
            onDismiss = viewModel::cancelNewQuestionDraft,
            onSave = viewModel::saveNewQuestion
        )
    }

    if (uiState.showTrialPaywall) {
        TrialPaywallSheet(onDismiss = viewModel::dismissTrialPaywall, onViewPlans = onOpenPricing)
    }

    if (uiState.showAiQuestionSheet) {
        OfflineExamAiSheet(uiState = uiState, viewModel = viewModel)
    }

    if (uiState.showScannerSheet) {
        OfflineExamScannerSheet(
            capturedUris = capturedUris,
            questionCount = uiState.photoQuestionCount,
            onQuestionCountChange = viewModel::onPhotoQuestionCountChange,
            isPreparing = isPreparingPhotos,
            isGenerating = uiState.isGeneratingFromPhotos,
            errorMessage = scannerLocalError ?: uiState.photoScanError,
            onAddPhoto = ::onAddPhotoTapped,
            onRemovePhoto = { uri -> capturedUris.remove(uri) },
            onGenerate = ::onGeneratePhotosTapped,
            onDismiss = viewModel::dismissScannerSheet
        )
    }

    if (showCameraRationale) {
        CameraRationaleDialog(
            onDismiss = { showCameraRationale = false },
            onAllow = {
                showCameraRationale = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { BlurBehindDialog(); Text("Delete \"${uiState.title}\"?") },
            text = { Text("This permanently deletes this offline exam and its saved questions link — the questions themselves stay in your question bank.") },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isDeleting,
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteExam()
                    }
                ) { Text("Delete", color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }, enabled = !uiState.isDeleting) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun OfflineExamQuestionRow(question: Question, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp, color = if (isSelected) BrandIndigoLight else SurfaceWhite)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

private fun typeLabel(type: QuestionType) = when (type) {
    QuestionType.SINGLE_CHOICE -> "Single Choice"
    QuestionType.MULTI_CHOICE -> "Multi Choice"
    QuestionType.FREE_TEXT -> "Free Text"
    QuestionType.FILL_IN_BLANK -> "Fill in Blank"
}

/** Small colored circle-icon badge used in the AI/Scanner sheet headers below — same "icon inside a
 *  tinted circle" language as MoreScreen's rows and QuickActionButton, rather than a bare title. */
@Composable
private fun SheetIconBadge(icon: ImageVector, background: Color, border: Color, tint: Color) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** "AI" button's topic-based generation sheet — same shape as CreateQuizScreen's AiQuestionSheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineExamAiSheet(uiState: CreateOfflineExamUiState, viewModel: CreateOfflineExamViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = viewModel::dismissAiQuestionSheet, sheetState = sheetState, containerColor = SurfaceWhite) {
        BlurBehindDialog()
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SheetIconBadge(icon = Icons.Default.AutoAwesome, background = AiCardBg, border = AiCardBorder, tint = BrandIndigo)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Add questions with AI", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Text("Straight into this exam's question list", color = TextSecondary, fontSize = 13.sp)
                }
                IconButton(onClick = viewModel::dismissAiQuestionSheet) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(20.dp))

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

/** "Scanner" button's photo-based generation sheet — capture flow mirrors AiQuizScreen's own
 *  camera plumbing (see CreateOfflineExamScreen's cameraLauncher/startCapture/onAddPhotoTapped). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineExamScannerSheet(
    capturedUris: List<Uri>,
    questionCount: Int,
    onQuestionCountChange: (Int) -> Unit,
    isPreparing: Boolean,
    isGenerating: Boolean,
    errorMessage: String?,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val busy = isPreparing || isGenerating

    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, sheetState = sheetState, containerColor = SurfaceWhite) {
        BlurBehindDialog()
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SheetIconBadge(icon = Icons.Default.CameraAlt, background = StatPurpleBg, border = StatPurpleBg, tint = StatPurpleIcon)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Scan question papers", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Text("Take photos of a paper — questions are extracted automatically", color = TextSecondary, fontSize = 13.sp)
                }
                IconButton(onClick = { if (!busy) onDismiss() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(20.dp))

            errorMessage?.let {
                ErrorBanner(message = it)
                Spacer(Modifier.height(12.dp))
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(capturedUris) { uri ->
                    CapturedPhotoThumb(uri = uri, enabled = !busy, onRemove = { onRemovePhoto(uri) })
                }
                if (capturedUris.size < AiAttachmentUtils.MAX_IMAGES) {
                    item { AddPhotoTile(enabled = !busy, onClick = onAddPhoto) }
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Questions", color = TextSecondary, fontSize = 13.sp)
                IntStepper(
                    value = questionCount,
                    onValueChange = onQuestionCountChange,
                    minValue = MIN_AI_QUESTION_COUNT,
                    maxValue = MAX_AI_QUESTION_COUNT
                )
            }
            Spacer(Modifier.height(20.dp))

            GradientButton(
                text = if (isPreparing) "Preparing…" else "Generate",
                onClick = onGenerate,
                leadingIcon = Icons.Default.AutoAwesome,
                enabled = capturedUris.isNotEmpty() && !busy,
                loading = busy,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CapturedPhotoThumb(uri: Uri, enabled: Boolean, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(72.dp)) {
        AsyncImage(
            model = uri,
            contentDescription = "Captured photo",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
        )
        IconButton(
            onClick = onRemove,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(22.dp)
                .background(SurfaceWhite, CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove photo", modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun AddPhotoTile(enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StatPurpleBg)
            .border(1.dp, StatPurpleIcon.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.CameraAlt, contentDescription = "Add photo", tint = StatPurpleIcon)
    }
}

/** Shown once per ask, right before the system camera permission dialog — lighter-weight version
 *  of AiQuizScreen's CameraPermissionRationaleSheet (no slide-in animation), same "why we want
 *  this" purpose. */
@Composable
private fun CameraRationaleDialog(onDismiss: () -> Unit, onAllow: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { BlurBehindDialog(); Text("Allow camera access") },
        text = { Text("We need your camera to photograph the question paper you're scanning.") },
        confirmButton = {
            TextButton(onClick = onAllow) { Text("Allow") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}

/** Duplicated from CreateQuizScreen's own NewQuestionSheet — same convention that file already
 *  follows (see its KDoc re: QuestionEditSheet.kt's copy) — minus the per-question negative
 *  marking reminder banner, since an Offline Exam has no quiz-wide negative marking mode setting
 *  for a single question to conflict with. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewOfflineExamQuestionSheet(
    draft: NewOfflineExamQuestionDraft,
    isSaving: Boolean,
    onUpdate: ((NewOfflineExamQuestionDraft) -> NewOfflineExamQuestionDraft) -> Unit,
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
                    Text("Add it to your bank and this exam", color = TextSecondary, fontSize = 13.sp)
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
                            RadioButton(
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
                OfflineExamAddRemoveOptionsRow(draft, onUpdate)
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
                    OfflineExamFilterChip(
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
            OfflineExamPointsStepper(
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
            if (draft.negativePoints > 0) {
                Spacer(Modifier.height(8.dp))
                Text("Deduct on wrong answer", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                OfflineExamPointsStepper(
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

@Composable
private fun OfflineExamFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
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

/** Duplicated from CreateQuizScreen's PointsStepper — see that file's own KDoc re: this exact
 *  duplication convention. */
@Composable
private fun OfflineExamPointsStepper(
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

/** Duplicated from CreateQuizScreen's IntStepper. */
@Composable
private fun IntStepper(value: Int, onValueChange: (Int) -> Unit, minValue: Int, maxValue: Int) {
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
private fun OfflineExamAddRemoveOptionsRow(draft: NewOfflineExamQuestionDraft, onUpdate: ((NewOfflineExamQuestionDraft) -> NewOfflineExamQuestionDraft) -> Unit) {
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
