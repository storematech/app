package com.quizmaker.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.NewQuestionDraft
import com.quizmaker.android.data.model.QuestionDifficulty
import com.quizmaker.android.data.model.QuestionType
import com.quizmaker.android.util.formatPoints

/**
 * "New/Edit Question" bottom sheet — shared by Question Bank (create or edit) and Revision (edit
 * only, tapping a bookmarked question). Pure state + callbacks so neither screen's ViewModel type
 * leaks into the other; both just supply their own `draft`/`updateDraft`/`saveDraft`/`dismissDraft`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionEditSheet(
    draft: NewQuestionDraft,
    isSaving: Boolean,
    onUpdateDraft: (transform: (NewQuestionDraft) -> NewQuestionDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = SurfaceWhite) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    val isEditing = draft.editingQuestionId != null
                    Text(
                        if (isEditing) "Edit Question" else "Create New Question",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Text(
                        if (isEditing) "Update this question in your bank" else "Add a question to your bank",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
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
                    onClick = { onUpdateDraft { it.copy(type = QuestionType.SINGLE_CHOICE) } }
                )
                QuestionTypeOption(
                    label = "Multiple Choice",
                    subtitle = "Many correct answers",
                    selected = draft.type == QuestionType.MULTI_CHOICE,
                    modifier = Modifier.weight(1f),
                    onClick = { onUpdateDraft { it.copy(type = QuestionType.MULTI_CHOICE) } }
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuestionTypeOption(
                    label = "Free Text",
                    subtitle = "Open written response",
                    selected = draft.type == QuestionType.FREE_TEXT,
                    modifier = Modifier.weight(1f),
                    onClick = { onUpdateDraft { it.copy(type = QuestionType.FREE_TEXT) } }
                )
                QuestionTypeOption(
                    label = "Fill in the Blanks",
                    subtitle = "Auto-graded text",
                    selected = draft.type == QuestionType.FILL_IN_BLANK,
                    modifier = Modifier.weight(1f),
                    onClick = { onUpdateDraft { it.copy(type = QuestionType.FILL_IN_BLANK) } }
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("QUESTION TEXT", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = draft.text,
                onValueChange = { text -> onUpdateDraft { it.copy(text = text) } },
                placeholder = { Text("Type your question here...") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            if (draft.type == QuestionType.SINGLE_CHOICE || draft.type == QuestionType.MULTI_CHOICE) {
                Spacer(Modifier.height(20.dp))
                Text("OPTIONS", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(8.dp))
                draft.options.forEachIndexed { index, optionText ->
                    Row(modifier = Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (draft.type == QuestionType.SINGLE_CHOICE) {
                            RadioButton(
                                selected = draft.correctOptionIndex == index,
                                onClick = { onUpdateDraft { it.copy(correctOptionIndex = index) } }
                            )
                        } else {
                            Checkbox(
                                checked = index in draft.correctOptionIndices,
                                onCheckedChange = { checked ->
                                    onUpdateDraft {
                                        val updated = if (checked) it.correctOptionIndices + index else it.correctOptionIndices - index
                                        it.copy(correctOptionIndices = updated)
                                    }
                                }
                            )
                        }
                        OutlinedTextField(
                            value = optionText,
                            onValueChange = { text ->
                                onUpdateDraft { it.copy(options = it.options.toMutableList().also { list -> list[index] = text }) }
                            },
                            placeholder = { Text("Option ${index + 1}") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (draft.options.size < 6) {
                        Text(
                            "+ Add option",
                            color = BrandIndigo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable {
                                onUpdateDraft { it.copy(options = it.options + "") }
                            }
                        )
                    }
                    if (draft.options.size > 2) {
                        Text(
                            "− Remove option",
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable {
                                onUpdateDraft {
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
            } else if (draft.type == QuestionType.FILL_IN_BLANK || draft.type == QuestionType.FREE_TEXT) {
                Spacer(Modifier.height(16.dp))
                Text("ANSWER", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = draft.freeTextAnswer,
                    onValueChange = { text -> onUpdateDraft { it.copy(freeTextAnswer = text) } },
                    placeholder = { Text("Expected answer") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("DIFFICULTY", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuestionDifficulty.entries.forEach { difficulty ->
                    QuestionEditFilterChip(
                        label = difficulty.value.replaceFirstChar { c -> c.uppercase() },
                        selected = draft.difficulty == difficulty,
                        onClick = { onUpdateDraft { it.copy(difficulty = difficulty) } }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("POINTS", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            PointsStepper(
                label = "Points",
                value = draft.points,
                onValueChange = { onUpdateDraft { d -> d.copy(points = it) } }
            )

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Enable Negative Marking",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = draft.negativePoints > 0,
                    onCheckedChange = { enabled ->
                        // Turning it off clears the value (rather than hiding a stale nonzero
                        // amount) so the DB row always reflects reality: negative_points > 0 IS
                        // the "enabled" flag, there's no separate boolean column.
                        onUpdateDraft { d -> d.copy(negativePoints = if (enabled) 0.25 else 0.0) }
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandIndigo)
                )
            }
            if (draft.negativePoints > 0) {
                Spacer(Modifier.height(8.dp))
                Text("Deduct on wrong answer", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                PointsStepper(
                    label = "Negative points",
                    value = draft.negativePoints,
                    onValueChange = { onUpdateDraft { d -> d.copy(negativePoints = it) } },
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
                    onUpdateDraft { it.copy(tags = text.split(",").map { tag -> tag.trim() }.filter { tag -> tag.isNotBlank() }) }
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
                onValueChange = { text -> onUpdateDraft { it.copy(explanation = text.ifBlank { null }) } },
                placeholder = { Text("Explain why this answer is correct...") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(90.dp)
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
                        text = if (draft.editingQuestionId != null) "Update" else "Save",
                        onClick = onSave,
                        loading = isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** +/- always moves by exactly 1 regardless of the current decimal value; the field in the middle
 *  can also be typed into directly, including decimals like "1.2". */
@Composable
private fun PointsStepper(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    minValue: Double = 0.25,
    maxValue: Double = 100.0
) {
    // Re-keyed on `value` (not on every keystroke) so the field only snaps back to the canonical
    // formatted value once a valid number actually lands — an in-progress string like "1." never
    // gets clobbered mid-type.
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

@Composable
private fun QuestionEditFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
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
