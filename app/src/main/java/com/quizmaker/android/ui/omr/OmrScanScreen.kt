package com.quizmaker.android.ui.omr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.core.theme.WarningAmber
import com.quizmaker.android.data.model.Learner
import com.quizmaker.android.data.model.OmrBubble
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.FilledPill
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.MathText
import com.quizmaker.android.ui.common.OutlinedPill
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.AiAttachmentUtils
import com.quizmaker.android.util.OmrSheetPdfExporter
import com.quizmaker.android.util.omr.OmrBubbleResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmrScanScreen(
    onNavigateBack: () -> Unit,
    viewModel: OmrScanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCaptureUri
        if (success && uri != null) {
            val bitmap = AiAttachmentUtils.decodeCapturedPhotoForOmr(context, uri)
            if (bitmap != null) viewModel.onPhotoCaptured(bitmap)
        }
    }

    fun startCapture() {
        val uri = AiAttachmentUtils.createCaptureUri(context)
        pendingCaptureUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCapture()
    }

    fun onTakePhotoTapped() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCapture()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun exportAndShareSheet() {
        scope.launch {
            val branding = viewModel.getPdfBranding()
            val result = OmrSheetPdfExporter.export(context, uiState.quizTitle, uiState.questions, branding)
            viewModel.onSheetExported(result.layout)
            context.startActivity(Intent.createChooser(result.shareIntent, "Print Answer Sheet"))
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Scan Answer Sheet", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (uiState.quizTitle.isNotBlank()) {
                            Text(uiState.quizTitle, fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LoadingCrossfade(
            isLoading = uiState.isLoading,
            modifier = Modifier.padding(padding),
            loadingContent = { ListScreenSkeleton(rowCount = 3, rowLineWidths = listOf(220.dp, 180.dp)) }
        ) {
            when {
                uiState.errorMessage != null && uiState.hasSheetLayout != false -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                    ErrorBanner(message = uiState.errorMessage!!, onRetry = { viewModel.refresh() })
                }
                uiState.hasSheetLayout == false -> NoSheetYetContent(
                    isExporting = uiState.isExportingSheet,
                    errorMessage = uiState.errorMessage,
                    onGenerateSheet = { exportAndShareSheet() }
                )
                else -> when (uiState.step) {
                    OmrScanStep.CAPTURE -> CaptureStepContent(onTakePhoto = { onTakePhotoTapped() })
                    OmrScanStep.SCANNING -> ScanningStepContent()
                    OmrScanStep.SCAN_FAILED -> ScanFailedContent(onRetake = { onTakePhotoTapped() })
                    OmrScanStep.REVIEW -> ReviewStepContent(
                        uiState = uiState,
                        onOverrideAnswer = viewModel::onOverrideAnswer,
                        onLearnerQueryChange = viewModel::onLearnerQueryChange,
                        onLearnerSelected = viewModel::onLearnerSelected,
                        onSubmit = viewModel::onSubmit,
                        onScanNext = viewModel::onScanNext,
                        onDone = onNavigateBack
                    )
                }
            }
        }
    }
}

@Composable
private fun NoSheetYetContent(isExporting: Boolean, errorMessage: String?, onGenerateSheet: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
        EmptyState(
            icon = Icons.Default.Description,
            title = "No answer sheet generated for this quiz yet",
            subtitle = "Generate a printable bubble sheet first, then come back here to scan filled-in copies."
        )
        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            ErrorBanner(message = errorMessage)
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        GradientButton(
            text = "Generate OMR Answer Sheet",
            onClick = onGenerateSheet,
            loading = isExporting,
            leadingIcon = Icons.Default.Description,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun CaptureStepContent(onTakePhoto: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EmptyState(
            icon = Icons.Default.CameraAlt,
            title = "Ready to scan",
            subtitle = "Lay the filled-in answer sheet flat and well-lit, with all 4 corner marks visible, then take a photo."
        )
        Spacer(Modifier.height(16.dp))
        GradientButton(
            text = "Take Photo",
            onClick = onTakePhoto,
            leadingIcon = Icons.Default.CameraAlt,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScanningStepContent() {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(color = BrandIndigo)
        Spacer(Modifier.height(12.dp))
        Text("Reading the sheet…", color = TextSecondary)
    }
}

@Composable
private fun ScanFailedContent(onRetake: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EmptyState(
            icon = Icons.Default.ErrorOutline,
            title = "Couldn't read this sheet",
            subtitle = "We couldn't find all 4 corner marks in the photo. Retake it flat, well-lit, with all corners visible in frame."
        )
        Spacer(Modifier.height(16.dp))
        GradientButton(text = "Retake Photo", onClick = onRetake, leadingIcon = Icons.Default.CameraAlt, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ReviewStepContent(
    uiState: OmrScanUiState,
    onOverrideAnswer: (String, List<String>) -> Unit,
    onLearnerQueryChange: (String) -> Unit,
    onLearnerSelected: (Learner) -> Unit,
    onSubmit: () -> Unit,
    onScanNext: () -> Unit,
    onDone: () -> Unit
) {
    val page = uiState.layout?.pages?.firstOrNull()
    val questionsById = uiState.questions.associateBy { it.id }
    val bubbledLayoutQuestions = page?.questions.orEmpty().filter { it.bubbles.isNotEmpty() }

    val canSubmit = uiState.unresolvedAmbiguous.isEmpty() &&
        (uiState.selectedLearner != null || uiState.learnerQuery.isNotBlank()) &&
        uiState.submitState != OmrSubmitState.Submitting

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (uiState.warpedBitmap != null) {
            item {
                Image(
                    bitmap = uiState.warpedBitmap.asImageBitmap(),
                    contentDescription = "Scanned sheet preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(uiState.warpedBitmap.width.toFloat() / uiState.warpedBitmap.height.toFloat())
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }

        item {
            Text("${bubbledLayoutQuestions.size} bubbled questions", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
        }

        items(bubbledLayoutQuestions, key = { it.questionId }) { layoutQuestion ->
            val question = questionsById[layoutQuestion.questionId]
            val bubbleResult = uiState.bubbledResults[layoutQuestion.questionId]
            val isMultiChoice = question?.type == QuestionType.MULTI_CHOICE
            val selected = uiState.selections[layoutQuestion.questionId].orEmpty()
            val isAmbiguous = layoutQuestion.questionId in uiState.unresolvedAmbiguous
            val isBlank = bubbleResult?.result is OmrBubbleResult.Blank && selected.isEmpty()

            QuestionReviewRow(
                questionNumber = bubbledLayoutQuestions.indexOf(layoutQuestion) + 1,
                questionText = question?.text.orEmpty(),
                bubbles = layoutQuestion.bubbles,
                selected = selected,
                isMultiChoice = isMultiChoice,
                isAmbiguous = isAmbiguous,
                isBlank = isBlank,
                onToggle = { optionId ->
                    val newSelection = if (isMultiChoice) {
                        if (optionId in selected) selected - optionId else selected + optionId
                    } else {
                        listOf(optionId)
                    }
                    onOverrideAnswer(layoutQuestion.questionId, newSelection)
                }
            )
        }

        item {
            LearnerAttributionSection(
                uiState = uiState,
                onLearnerQueryChange = onLearnerQueryChange,
                onLearnerSelected = onLearnerSelected
            )
        }

        if (uiState.submitState is OmrSubmitState.Error) {
            item { ErrorBanner(message = (uiState.submitState as OmrSubmitState.Error).message) }
        }

        item {
            when (uiState.submitState) {
                is OmrSubmitState.Success -> Column {
                    Text("Saved!", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    GradientButton(text = "Scan Next Paper", onClick = onScanNext, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Done") }
                }
                else -> GradientButton(
                    text = if (uiState.submitState == OmrSubmitState.Submitting) "Saving…" else "Save & Score",
                    onClick = onSubmit,
                    enabled = canSubmit,
                    loading = uiState.submitState == OmrSubmitState.Submitting,
                    leadingIcon = Icons.Default.CheckCircle,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun QuestionReviewRow(
    questionNumber: Int,
    questionText: String,
    bubbles: List<OmrBubble>,
    selected: List<String>,
    isMultiChoice: Boolean,
    isAmbiguous: Boolean,
    isBlank: Boolean,
    onToggle: (String) -> Unit
) {
    val flagColor = when {
        isAmbiguous -> ErrorRed
        isBlank -> WarningAmber
        else -> null
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 2.dp)
            .then(if (flagColor != null) Modifier.border(1.5.dp, flagColor, RoundedCornerShape(16.dp)) else Modifier)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledPill(text = "Q$questionNumber")
            Spacer(Modifier.width(8.dp))
            if (isAmbiguous) OutlinedPill(text = "Needs review", borderColor = ErrorRed, contentColor = ErrorRed)
            else if (isBlank) OutlinedPill(text = "Blank", borderColor = WarningAmber, contentColor = WarningAmber)
        }
        Spacer(Modifier.height(6.dp))
        MathText(text = questionText, color = TextPrimary, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            bubbles.forEach { bubble ->
                val isSelected = bubble.optionId in selected
                Box(modifier = Modifier.clickable { onToggle(bubble.optionId) }) {
                    if (isSelected) {
                        FilledPill(text = bubble.label, containerColor = BrandIndigo)
                    } else {
                        OutlinedPill(text = bubble.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun LearnerAttributionSection(
    uiState: OmrScanUiState,
    onLearnerQueryChange: (String) -> Unit,
    onLearnerSelected: (Learner) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Student", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.learnerQuery,
            onValueChange = onLearnerQueryChange,
            label = { Text("Student name") },
            placeholder = { Text("Search or type a name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (uiState.selectedLearner != null) {
            Spacer(Modifier.height(6.dp))
            Text("Matched: ${uiState.selectedLearner.name}", color = SuccessGreen, fontSize = 12.sp)
        } else if (uiState.filteredLearners.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Column {
                uiState.filteredLearners.take(5).forEach { learner ->
                    Text(
                        learner.name,
                        color = BrandIndigo,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLearnerSelected(learner) }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}
