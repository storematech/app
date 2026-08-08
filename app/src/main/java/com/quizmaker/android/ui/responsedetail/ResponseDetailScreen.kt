package com.quizmaker.android.ui.responsedetail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.PdfRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.StatAmberBg
import com.quizmaker.android.core.theme.StatAmberIcon
import com.quizmaker.android.core.theme.StatBlueBg
import com.quizmaker.android.core.theme.StatBlueIcon
import com.quizmaker.android.core.theme.StatGreenBg
import com.quizmaker.android.core.theme.StatGreenIcon
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.core.theme.WarningAmber
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.data.model.AnswerDetail
import com.quizmaker.android.data.model.AnswerStatus
import com.quizmaker.android.ui.common.CsvFileIcon
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.StatTile
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.PdfPrinter
import com.quizmaker.android.util.ResponseDetailCsvExporter
import com.quizmaker.android.util.ResponseDetailPdfExporter
import androidx.core.content.FileProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponseDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ResponseDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Details", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        uiState.data?.userName?.let {
                            val short = if (it.length > 10) "${it.take(10)}..." else it
                            Text(short, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    val data = uiState.data
                    if (data != null && data.answers.isNotEmpty()) {
                        IconButton(onClick = {
                            val file = ResponseDetailPdfExporter.export(context, data)
                            PdfPrinter.print(context, "Response - ${data.userName}", file)
                        }) {
                            Icon(Icons.Default.Print, contentDescription = "Print")
                        }
                        IconButton(onClick = {
                            val file = ResponseDetailPdfExporter.export(context, data)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share response"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = {
                            val file = ResponseDetailPdfExporter.export(context, data)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Open PDF"))
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Open PDF", tint = PdfRed)
                        }
                        IconButton(onClick = {
                            val intent = ResponseDetailCsvExporter.export(context, data)
                            context.startActivity(Intent.createChooser(intent, "Export CSV"))
                        }) {
                            CsvFileIcon(contentDescription = "Export CSV")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LoadingCrossfade(
            isLoading = uiState.isLoading,
            modifier = Modifier.padding(padding),
            loadingContent = { ListScreenSkeleton(statTileCount = 3, rowCount = 5, rowLineWidths = listOf(220.dp, 160.dp, 140.dp)) }
        ) {
            when {
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                    ErrorBanner(message = uiState.errorMessage!!)
                }
                uiState.data != null -> {
                    val data = uiState.data!!
                    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .elevatedSurface(shape = RoundedCornerShape(18.dp), elevation = 3.dp)
                                    .padding(16.dp)
                            ) {
                                Text(data.userName, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                                Text(data.userEmail, color = TextSecondary, fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatTile(
                                    icon = Icons.Default.Assessment,
                                    iconBg = StatBlueBg,
                                    iconTint = StatBlueIcon,
                                    label = "Score",
                                    value = "${data.scorePercent}%",
                                    modifier = Modifier.weight(1f)
                                )
                                StatTile(
                                    icon = Icons.Default.Timer,
                                    iconBg = StatAmberBg,
                                    iconTint = StatAmberIcon,
                                    label = "Time Taken",
                                    value = data.timeSeconds?.let { "${it / 60}m ${it % 60}s" } ?: "-",
                                    modifier = Modifier.weight(1f)
                                )
                                StatTile(
                                    icon = Icons.Default.Assessment,
                                    iconBg = StatGreenBg,
                                    iconTint = StatGreenIcon,
                                    label = "Questions",
                                    value = data.answers.size.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "Question by Question Analysis",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        itemsIndexed(data.answers) { index, answer ->
                            AnswerDetailCard(index = index, answer = answer)
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnswerDetailCard(index: Int, answer: AnswerDetail) {
    val (bandColor, statusLabel) = when (answer.status) {
        AnswerStatus.CORRECT -> SuccessGreen to "Correct"
        AnswerStatus.PARTIAL -> WarningAmber to "Partial"
        AnswerStatus.INCORRECT -> ErrorRed to "Incorrect"
        AnswerStatus.UNGRADED -> TextSecondary to "Ungraded"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Q${index + 1}. ${answer.questionText}",
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(bandColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(statusLabel, color = androidx.compose.ui.graphics.Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Submitted Answer", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(answer.studentAnswer, color = TextPrimary, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Text("Correct Answer", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(answer.correctAnswer, color = SuccessGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(
            if (answer.status == AnswerStatus.UNGRADED) "Ungraded" else "${answer.pointsEarned}/${answer.maxPoints} points",
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}
