package com.quizmaker.android.ui.importquestions

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.QuestionExcelTemplateWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportQuestionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ImportQuestionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                ?: "Selected file"
            viewModel.onFileSelected(name) { context.contentResolver.openInputStream(uri) }
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Import Questions", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                "Add questions in bulk from an Excel file",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    val intent = QuestionExcelTemplateWriter.export(context)
                    context.startActivity(Intent.createChooser(intent, "Download template"))
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Download Template")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { filePicker.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) },
                enabled = !uiState.isImporting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Select Excel File (.xlsx)")
            }

            uiState.selectedFileName?.let { name ->
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppBackground)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(name, color = TextPrimary, fontSize = 13.sp, maxLines = 1)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = viewModel::importSelectedFile,
                    enabled = !uiState.isImporting,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Import Questions", fontWeight = FontWeight.Bold)
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                Spacer(Modifier.height(16.dp))
                ErrorBanner(message = message)
            }

            if (uiState.hasImported) {
                Spacer(Modifier.height(20.dp))
                ImportResultsCard(successCount = uiState.successCount, failedRows = uiState.failedRows)
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Columns: Question, Type (option / free-text / multi-choice), Option A–D, " +
                    "Correct Answer (A–D), Points, Tags, Difficulty (easy / medium / hard).",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ImportResultsCard(successCount: Int, failedRows: List<com.quizmaker.android.util.FailedImportRow>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("$successCount question${if (successCount == 1) "" else "s"} imported", color = TextPrimary, fontWeight = FontWeight.Medium)
        }
        if (failedRows.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("${failedRows.size} row${if (failedRows.size == 1) "" else "s"} skipped", color = TextPrimary, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                failedRows.take(10).forEach { failure ->
                    val label = if (failure.row > 0) "Row ${failure.row}: ${failure.reason}" else failure.reason
                    Text(label, color = TextSecondary, fontSize = 12.sp)
                }
                if (failedRows.size > 10) {
                    Text("+ ${failedRows.size - 10} more", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}
