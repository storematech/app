package com.quizmaker.android.ui.manualmarking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.core.theme.WarningAmber
import com.quizmaker.android.data.model.MarkingItem
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.formatPoints

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualMarkingScreen(
    onNavigateBack: () -> Unit,
    viewModel: ManualMarkingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Manual Marking", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandIndigo)
            }
            uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
                ErrorBanner(message = uiState.errorMessage!!)
            }
            uiState.items.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    icon = Icons.Default.Edit,
                    title = "Nothing to mark yet",
                    subtitle = "Free-text answers show up here once participants submit this quiz."
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(uiState.items, key = { it.answerDetailId }) { item ->
                    MarkingCard(
                        item = item,
                        isSaving = uiState.savingItemId == item.answerDetailId,
                        onSubmit = { points -> viewModel.submitMarking(item, points) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkingCard(item: MarkingItem, isSaving: Boolean, onSubmit: (Double) -> Unit) {
    var points by remember(item.answerDetailId) { mutableStateOf(item.pointsEarned.coerceIn(0.0, item.maxPoints)) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(item.participantLabel, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            StatusPill(isPending = item.isPending)
        }
        Spacer(Modifier.height(8.dp))
        Text(item.questionText, color = TextPrimary, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))
        Text("ANSWER", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(4.dp))
        Text(item.studentAnswer, color = TextPrimary, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Points (max ${item.maxPoints.formatPoints()})", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            MarkingPointsStepper(value = points, maxValue = item.maxPoints, onValueChange = { points = it })
        }
        Spacer(Modifier.height(14.dp))
        GradientButton(
            text = if (item.isPending) "Save Marking" else "Update Marking",
            onClick = { onSubmit(points) },
            loading = isSaving,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatusPill(isPending: Boolean) {
    val (bg, fg, label) = if (isPending) Triple(WarningAmber.copy(alpha = 0.15f), WarningAmber, "Marking pending")
        else Triple(SuccessGreen.copy(alpha = 0.15f), SuccessGreen, "Marked")
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Same +/- + type-directly shape used elsewhere in the app (QuestionEditSheet/CreateQuizScreen's
 *  own PointsStepper copies) — a fresh one here too since 0 is a valid mark (those default to a
 *  0.25 floor, which doesn't fit "award zero points" as a legitimate outcome). */
@Composable
private fun MarkingPointsStepper(value: Double, maxValue: Double, onValueChange: (Double) -> Unit) {
    var text by remember(value) { mutableStateOf(value.formatPoints()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onValueChange((value - 1.0).coerceIn(0.0, maxValue)) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease points", tint = BrandIndigo)
        }
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input
                input.toDoubleOrNull()?.let { onValueChange(it.coerceIn(0.0, maxValue)) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.width(76.dp)
        )
        IconButton(
            onClick = { onValueChange((value + 1.0).coerceIn(0.0, maxValue)) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase points", tint = BrandIndigo)
        }
    }
}
