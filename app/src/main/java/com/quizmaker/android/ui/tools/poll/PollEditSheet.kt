package com.quizmaker.android.ui.tools.poll

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.Poll
import com.quizmaker.android.data.model.PollOption
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.util.formatShortDate
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

private const val MAX_OPTIONS = 8

private fun defaultOptions(): List<PollOption> = listOf(
    PollOption(id = UUID.randomUUID().toString(), label = ""),
    PollOption(id = UUID.randomUUID().toString(), label = "")
)

/** "New/Edit Poll" bottom sheet — [initialPoll] null means creating; non-null pre-fills for editing. Same shape as OnboardingFormEditSheet/FeedbackFormEditSheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollEditSheet(
    initialPoll: Poll?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (question: String, description: String, options: List<PollOption>, allowMultiple: Boolean, showResults: Boolean, closesAt: String?, isActive: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var question by remember(initialPoll) { mutableStateOf(initialPoll?.question.orEmpty()) }
    var description by remember(initialPoll) { mutableStateOf(initialPoll?.description.orEmpty()) }
    var options by remember(initialPoll) { mutableStateOf(initialPoll?.options ?: defaultOptions()) }
    var allowMultiple by remember(initialPoll) { mutableStateOf(initialPoll?.allowMultiple ?: false) }
    var showResults by remember(initialPoll) { mutableStateOf(initialPoll?.showResults ?: true) }
    var hasDeadline by remember(initialPoll) { mutableStateOf(initialPoll?.closesAt != null) }
    var closesAt by remember(initialPoll) { mutableStateOf(initialPoll?.closesAt) }
    var isActive by remember(initialPoll) { mutableStateOf(initialPoll?.isActive ?: true) }
    val isEditing = initialPoll != null

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = SurfaceWhite) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isEditing) "Edit Poll" else "New Poll",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Text(
                        if (isEditing) "Update this poll" else "Ask a quick question and share the link",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("QUESTION", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                placeholder = { Text("e.g. Which topic should we cover next?") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("WRITE POLL FOR (OPTIONAL)", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Any extra context for voters") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            Text("OPTIONS", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            options.forEachIndexed { index, option ->
                Row(modifier = Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = option.label,
                        onValueChange = { text -> options = options.toMutableList().also { it[index] = option.copy(label = text) } },
                        placeholder = { Text("Option ${index + 1}") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    if (options.size > 2) {
                        IconButton(onClick = { options = options.filterIndexed { i, _ -> i != index } }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove option", tint = TextSecondary)
                        }
                    }
                }
            }
            if (options.size < MAX_OPTIONS) {
                Text(
                    "+ Add option",
                    color = BrandIndigo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { options = options + PollOption(id = UUID.randomUUID().toString(), label = "") }
                )
            }

            Spacer(Modifier.height(16.dp))
            ToggleRow(
                title = "Allow multiple selections",
                subtitle = "Voters can pick more than one option.",
                checked = allowMultiple,
                onCheckedChange = { allowMultiple = it }
            )
            Spacer(Modifier.height(12.dp))
            ToggleRow(
                title = "Show results to voters",
                subtitle = "Voters see live results after voting.",
                checked = showResults,
                onCheckedChange = { showResults = it }
            )

            Spacer(Modifier.height(12.dp))
            ToggleRow(
                title = "Set a closing date",
                subtitle = "Stop accepting votes after a deadline.",
                checked = hasDeadline,
                onCheckedChange = { checked ->
                    hasDeadline = checked
                    if (!checked) closesAt = null
                }
            )
            if (hasDeadline) {
                Spacer(Modifier.height(10.dp))
                closesAt?.let {
                    Text("Closes ${formatShortDate(it)}", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeadlineChip("1 Day", onClick = { closesAt = (Clock.System.now() + 1.days).toString() })
                    DeadlineChip("3 Days", onClick = { closesAt = (Clock.System.now() + 3.days).toString() })
                    DeadlineChip("7 Days", onClick = { closesAt = (Clock.System.now() + 7.days).toString() })
                    DeadlineChip("30 Days", onClick = { closesAt = (Clock.System.now() + 30.days).toString() })
                }
            }

            Spacer(Modifier.height(12.dp))
            ToggleRow(
                title = "Accepting votes",
                subtitle = "Turn off to close the poll.",
                checked = isActive,
                onCheckedChange = { isActive = it }
            )

            Spacer(Modifier.height(24.dp))
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
                    GradientButton(
                        text = if (isEditing) "Save Changes" else "Create Poll",
                        onClick = { onSave(question, description, options, allowMultiple, showResults, closesAt, isActive) },
                        loading = isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandIndigo)
        )
    }
}

@Composable
private fun DeadlineChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AppBackground)
            .border(1.dp, BorderGray, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
