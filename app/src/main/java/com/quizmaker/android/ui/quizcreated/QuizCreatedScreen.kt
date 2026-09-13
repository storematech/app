package com.quizmaker.android.ui.quizcreated

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.Question
import com.quizmaker.android.data.model.Quiz
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.SuccessCheckmark
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.MasterPaperMode
import com.quizmaker.android.util.MasterPaperPdfExporter
import com.quizmaker.android.util.PdfBranding
import com.quizmaker.android.util.PdfPrinter
import com.quizmaker.android.util.QrCodeGenerator
import com.quizmaker.android.util.QrFlyerPdfExporter
import kotlinx.coroutines.launch

/**
 * Shown once, right after creating a brand-new quiz — a shareable "flyer" (QR + downloads +
 * Master Paper export), not the ongoing quiz-management screen (that's QuizDetailScreen, reached
 * later from the quiz list). Both Done and the hardware/gesture back button land on the Quizzes
 * tab — see NavGraph.kt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizCreatedScreen(
    onDone: () -> Unit,
    viewModel: QuizCreatedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(onBack = onDone)

    Scaffold(containerColor = AppBackground) { padding ->
        LoadingCrossfade(isLoading = uiState.isLoading, modifier = Modifier.padding(padding)) {
            when {
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(20.dp)) {
                    ErrorBanner(message = uiState.errorMessage!!)
                }
                uiState.quiz != null -> QuizCreatedContent(
                    quiz = uiState.quiz!!,
                    questions = uiState.questions,
                    onDone = onDone,
                    getPdfBranding = viewModel::getPdfBranding,
                    onShared = viewModel::logShared
                )
            }
        }
    }
}

@Composable
private fun QuizCreatedContent(
    quiz: Quiz,
    questions: List<Question>,
    onDone: () -> Unit,
    getPdfBranding: suspend () -> PdfBranding,
    onShared: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val qrBitmap = remember(quiz.shareUrl) { QrCodeGenerator.generate(quiz.shareUrl) }

    fun exportMasterPaper(mode: MasterPaperMode) {
        scope.launch {
            val branding = getPdfBranding()
            val intent = MasterPaperPdfExporter.export(context, quiz.title, questions, mode, branding)
            context.startActivity(Intent.createChooser(intent, "Export"))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))
        SuccessCheckmark(size = 84.dp)
        Spacer(Modifier.height(16.dp))
        Text("Quiz Created!", fontFamily = PoppinsFamily, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            quiz.title.ifBlank { "Untitled quiz" },
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        // QR code
        Column(
            modifier = Modifier.fillMaxWidth().elevatedSurface(shape = RoundedCornerShape(20.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "QR code linking to this quiz",
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                    .padding(10.dp)
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
                    .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    quiz.shareUrl,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                LinkIconButton(icon = Icons.Default.ContentCopy, contentDescription = "Copy link") {
                    onShared("copy")
                    clipboardManager.setText(AnnotatedString(quiz.shareUrl))
                }
                LinkIconButton(icon = Icons.Default.OpenInBrowser, contentDescription = "Open in browser") {
                    onShared("view")
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(quiz.shareUrl)))
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        onShared("qr_code")
                        val intent = QrCodeGenerator.sharePng(context, qrBitmap, quiz.title)
                        context.startActivity(Intent.createChooser(intent, "Download QR Code"))
                    },
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("QR Code")
                }
                OutlinedButton(
                    onClick = {
                        onShared("print")
                        scope.launch {
                            val branding = getPdfBranding()
                            val file = QrFlyerPdfExporter.renderFile(context, quiz.title, quiz.shareUrl, qrBitmap, branding)
                            PdfPrinter.print(context, quiz.title, file)
                        }
                    },
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Print")
                }
            }
            Spacer(Modifier.height(10.dp))
            GradientButton(
                text = "Share Link",
                leadingIcon = Icons.Default.Share,
                onClick = {
                    onShared("link")
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Take my quiz \"${quiz.title}\": ${quiz.shareUrl}")
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share quiz"))
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(20.dp))

        // Master Paper
        Column(
            modifier = Modifier.fillMaxWidth().elevatedSurface(shape = RoundedCornerShape(20.dp)).padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Master Paper", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            }
            Spacer(Modifier.height(4.dp))
            Text("Export this quiz's questions as a print-ready paper", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))

            GradientButton(
                text = "Export with Answers",
                onClick = { exportMasterPaper(MasterPaperMode.WITH_ANSWERS) },
                leadingIcon = Icons.Default.Download,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { exportMasterPaper(MasterPaperMode.WITHOUT_ANSWERS) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export without Answers")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { exportMasterPaper(MasterPaperMode.OFFLINE) },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Offline Exam Paper")
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = BorderGray)
        Spacer(Modifier.height(20.dp))

        GradientButton(text = "Back to Quiz List", onClick = onDone, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LinkIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = BrandIndigo, modifier = Modifier.size(18.dp))
    }
}
