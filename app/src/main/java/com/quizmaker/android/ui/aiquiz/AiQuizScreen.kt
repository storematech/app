package com.quizmaker.android.ui.aiquiz

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.util.AiAttachmentUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiQuizScreen(
    onNavigateToCreateQuiz: (List<String>) -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: AiQuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    val capturedUris = remember { mutableStateListOf<Uri>() }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var isPreparing by remember { mutableStateOf(false) }

    fun clearAttachments() {
        selectedPdfUri = null
        capturedUris.clear()
        viewModel.clearAttachment()
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            capturedUris.clear()
            selectedPdfUri = uri
            localError = null
            viewModel.setPdfAttachment(AiAttachmentUtils.queryFileName(context, uri))
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCaptureUri
        if (success && uri != null) {
            selectedPdfUri = null
            capturedUris.add(uri)
            localError = null
            viewModel.setImagesAttachment(capturedUris.size)
        }
    }

    fun startCapture() {
        val uri = AiAttachmentUtils.createCaptureUri(context)
        pendingCaptureUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCapture() else localError = "Camera permission is needed to take photos."
    }

    fun onClickPhotoTapped() {
        localError = null
        if (capturedUris.size >= AiAttachmentUtils.MAX_IMAGES) {
            localError = "You can add up to ${AiAttachmentUtils.MAX_IMAGES} photos."
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun onGenerateTapped() {
        localError = null
        when (uiState.attachmentKind) {
            AiAttachmentKind.PDF -> {
                val uri = selectedPdfUri
                if (uri == null) {
                    localError = "Please choose a PDF first."
                } else {
                    scope.launch {
                        isPreparing = true
                        val result = withContext(Dispatchers.IO) { AiAttachmentUtils.readPdfAsBase64(context, uri) }
                        isPreparing = false
                        when (result) {
                            is AiAttachmentUtils.AttachmentResult.Success -> viewModel.generateFromPdf(result.base64)
                            is AiAttachmentUtils.AttachmentResult.Error -> localError = result.message
                        }
                    }
                }
            }
            AiAttachmentKind.IMAGES -> {
                if (capturedUris.isEmpty()) {
                    localError = "Please take at least one photo."
                } else {
                    val urisSnapshot = capturedUris.toList()
                    scope.launch {
                        isPreparing = true
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
                        isPreparing = false
                        if (failure != null) localError = failure else viewModel.generateFromImages(encoded)
                    }
                }
            }
            null -> viewModel.generate()
        }
    }

    LaunchedEffect(uiState.navigateToCreateQuizWith) {
        uiState.navigateToCreateQuizWith?.let { ids ->
            clearAttachments()
            viewModel.consumeNavigation()
            onNavigateToCreateQuiz(ids)
        }
    }

    LaunchedEffect(uiState.addQuestionsCompleted) {
        if (uiState.addQuestionsCompleted) {
            clearAttachments()
            viewModel.consumeNavigation()
            onNavigateBack()
        }
    }

    val busy = uiState.isGenerating || isPreparing
    val displayError = localError ?: uiState.errorMessage

    // The bottom nav bar's own Scaffold (NavGraph) already reserves the system nav-bar inset;
    // reserving it again here would leave a redundant empty strip above the tab bar.
    Scaffold(containerColor = AppBackground, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(BrandIndigoLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (viewModel.isAddQuestionsMode) "Create Questions with AI" else "Create Quiz with AI",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Text("Prompt it, or attach a PDF / photos", color = TextSecondary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(18.dp))

            if (viewModel.showModeStrip) {
                ModeStrip(label = viewModel.modeStripLabel, onClose = onNavigateBack)
                Spacer(Modifier.height(14.dp))
            }

            displayError?.let {
                ErrorBanner(message = it)
                Spacer(Modifier.height(12.dp))
            }

            // ChatGPT-style composer: one card holds attachments, the text field, and the
            // action row (attach buttons + question count + send) so it reads as a single input.
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderGray),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (uiState.attachmentKind == AiAttachmentKind.PDF && selectedPdfUri != null) {
                        PdfAttachmentChip(name = uiState.attachmentLabel.orEmpty(), onRemove = { clearAttachments() })
                        Spacer(Modifier.height(10.dp))
                    }
                    if (uiState.attachmentKind == AiAttachmentKind.IMAGES && capturedUris.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(capturedUris.toList()) { uri ->
                                CapturedPhotoThumb(
                                    uri = uri,
                                    onRemove = {
                                        capturedUris.remove(uri)
                                        if (capturedUris.isEmpty()) viewModel.clearAttachment() else viewModel.setImagesAttachment(capturedUris.size)
                                    }
                                )
                            }
                            if (capturedUris.size < AiAttachmentUtils.MAX_IMAGES) {
                                item { AddMorePhotoTile(onClick = ::onClickPhotoTapped) }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = uiState.prompt,
                        onValueChange = { viewModel.onPromptChange(it); localError = null },
                        placeholder = {
                            Text(
                                when {
                                    uiState.attachmentKind != null -> "Optional instructions, e.g. \"Focus on chapter 3\""
                                    viewModel.isAddQuestionsMode -> "Create questions for 5th class science, biology chapter…"
                                    else -> "Create a quiz for 5th class science, biology chapter…"
                                },
                                color = TextSecondary
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = TextPrimary),
                        minLines = 2,
                        maxLines = 6,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        ComposerIconButton(icon = Icons.Default.UploadFile, contentDescription = "Upload PDF", enabled = !busy) {
                            localError = null
                            pdfPickerLauncher.launch(arrayOf("application/pdf"))
                        }
                        Spacer(Modifier.width(8.dp))
                        ComposerIconButton(icon = Icons.Default.CameraAlt, contentDescription = "Click Photo", enabled = !busy) {
                            onClickPhotoTapped()
                        }

                        Spacer(Modifier.weight(1f))

                        Text("Questions", color = TextSecondary, fontSize = 11.sp)
                        Spacer(Modifier.width(6.dp))
                        QuestionCountStepper(
                            count = uiState.questionCount,
                            enabled = !busy,
                            onDecrement = { viewModel.onQuestionCountChange(uiState.questionCount - 1) },
                            onIncrement = { viewModel.onQuestionCountChange(uiState.questionCount + 1) }
                        )
                        Spacer(Modifier.width(10.dp))

                        SendButton(
                            enabled = uiState.canGenerate && !busy,
                            loading = busy,
                            icon = if (uiState.hasReview) Icons.Default.Refresh else Icons.Default.ArrowUpward,
                            onClick = ::onGenerateTapped
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    isPreparing -> "Preparing your file…"
                    uiState.isGenerating -> "Generating your quiz…"
                    uiState.hasReview -> "Uncheck any questions you don't want, then create the quiz."
                    else -> "Questions are saved to your question bank. Files aren't stored."
                },
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            if (uiState.hasReview && !busy) {
                Spacer(Modifier.height(20.dp))
                ReviewSection(
                    questions = uiState.reviewQuestions,
                    selectedIds = uiState.selectedReviewIds,
                    isAddQuestionsMode = viewModel.isAddQuestionsMode,
                    onToggle = viewModel::toggleReviewQuestion,
                    onToggleAll = {
                        if (uiState.selectedReviewIds.size == uiState.reviewQuestions.size) viewModel.deselectAllReview() else viewModel.selectAllReview()
                    },
                    onConfirm = viewModel::confirmSelection
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ReviewSection(
    questions: List<Question>,
    selectedIds: Set<String>,
    isAddQuestionsMode: Boolean,
    onToggle: (String) -> Unit,
    onToggleAll: () -> Unit,
    onConfirm: () -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Generated Questions", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Text("${selectedIds.size} of ${questions.size} selected", color = TextSecondary, fontSize = 12.sp)
            }
            Text(
                if (selectedIds.size == questions.size) "Deselect all" else "Select all",
                color = BrandIndigo,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onToggleAll)
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            questions.forEachIndexed { index, question ->
                ReviewQuestionTicket(
                    question = question,
                    index = index,
                    isSelected = question.id in selectedIds,
                    onToggle = { onToggle(question.id) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        GradientButton(
            text = if (isAddQuestionsMode) "Add Questions (${selectedIds.size})" else "Create Quiz (${selectedIds.size})",
            onClick = onConfirm,
            leadingIcon = if (isAddQuestionsMode) Icons.Default.Add else Icons.Default.AutoAwesome,
            enabled = selectedIds.isNotEmpty(),
            loading = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ModeStrip(label: String, onClose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(BrandIndigoLight)
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Text(label, color = BrandIndigo, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(SurfaceWhite)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = BrandIndigo, modifier = Modifier.size(11.dp))
        }
    }
}

@Composable
private fun ReviewQuestionTicket(question: Question, index: Int, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) BrandIndigoLight.copy(alpha = 0.45f) else SurfaceWhite)
            .border(1.dp, if (isSelected) BrandIndigo.copy(alpha = 0.35f) else BorderGray, RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = BrandIndigo)
        )
        Spacer(Modifier.width(2.dp))
        Column(modifier = Modifier.weight(1f).padding(top = 10.dp)) {
            Text(
                "Q${index + 1}. ${question.text}",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            question.options.firstOrNull { it.isCorrect }?.let { correct ->
                Spacer(Modifier.height(4.dp))
                Text("✓ ${correct.text}", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ComposerIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(AppBackground)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = TextSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun QuestionCountStepper(count: Int, enabled: Boolean, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AppBackground)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        StepperButton(icon = Icons.Default.Remove, enabled = enabled && count > MIN_AI_QUESTION_COUNT, onClick = onDecrement)
        Text(
            "$count",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        StepperButton(icon = Icons.Default.Add, enabled = enabled && count < MAX_AI_QUESTION_COUNT, onClick = onIncrement)
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun SendButton(enabled: Boolean, loading: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (enabled || loading) BrandIndigo else BorderGray)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(color = SurfaceWhite, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        } else {
            Icon(
                icon,
                contentDescription = "Generate",
                tint = if (enabled) SurfaceWhite else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PdfAttachmentChip(name: String, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BrandIndigoLight)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun CapturedPhotoThumb(uri: Uri, onRemove: () -> Unit) {
    Box(modifier = Modifier.size(64.dp)) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(TextPrimary.copy(alpha = 0.6f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(11.dp))
        }
    }
}

@Composable
private fun AddMorePhotoTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.CameraAlt, contentDescription = "Add photo", tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}
