package com.quizmaker.android.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.BlurBehindDialog
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.util.formatShortDate
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Replaces the plain Material DropdownMenu this used to open — a proper bottom sheet with
 * selectable rows plus an inline "Custom Range" picker (two single-date pickers, same pattern as
 * ui/learners/ExportDateRangeDialog.kt — no precedent in this app for Material3's
 * DateRangePicker, and it doesn't fit well inside a sheet).
 *
 * [customEnd] is stored exclusive (start of the day *after* the picked "To" date, see
 * DashboardViewModel.onCustomRangeSelected) so it can be compared directly against
 * `completedAt < end`; this reads it back by subtracting a day, so the picker re-opens showing the
 * inclusive date the user actually picked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardDateRangeSheet(
    selectedRange: DashboardDateRange,
    customStart: Instant?,
    customEnd: Instant?,
    onSelectPreset: (DashboardDateRange) -> Unit,
    onApplyCustomRange: (start: Instant, end: Instant) -> Unit,
    onDismiss: () -> Unit
) {
    var customExpanded by remember { mutableStateOf(selectedRange == DashboardDateRange.CUSTOM) }
    var fromMillis by remember {
        mutableStateOf(customStart?.toEpochMilliseconds() ?: (Clock.System.now() - 7.days).toEpochMilliseconds())
    }
    var toMillis by remember {
        mutableStateOf((customEnd?.minus(1.days) ?: Clock.System.now()).toEpochMilliseconds())
    }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    // Default confirmValueChange (unlike ShareQuizSheet/LearnerFormDialog, which deliberately block
    // it) — tapping outside or swiping down should dismiss this sheet like any other.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = SurfaceWhite) {
        BlurBehindDialog()
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(BrandIndigoLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Select Date Range", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
            }
            Spacer(Modifier.height(20.dp))

            listOf(
                DashboardDateRange.LAST_7_DAYS,
                DashboardDateRange.LAST_30_DAYS,
                DashboardDateRange.LAST_90_DAYS,
                DashboardDateRange.ALL_TIME
            ).forEach { range ->
                DateRangeOptionRow(
                    label = range.label,
                    selected = selectedRange == range && !customExpanded,
                    onClick = { onSelectPreset(range); onDismiss() }
                )
                Spacer(Modifier.height(10.dp))
            }

            DateRangeOptionRow(
                label = "Custom Range",
                selected = customExpanded,
                onClick = { customExpanded = !customExpanded }
            )

            AnimatedVisibility(visible = customExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("From", color = TextSecondary, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            DateChip(formatShortDate(Instant.fromEpochMilliseconds(fromMillis))) { showFromPicker = true }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("To", color = TextSecondary, fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            DateChip(formatShortDate(Instant.fromEpochMilliseconds(toMillis))) { showToPicker = true }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    GradientButton(
                        text = "Apply",
                        enabled = fromMillis <= toMillis,
                        onClick = {
                            val start = Instant.fromEpochMilliseconds(fromMillis)
                            val end = Instant.fromEpochMilliseconds(toMillis) + 1.days
                            onApplyCustomRange(start, end)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showFromPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = fromMillis)
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { fromMillis = it }
                    showFromPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showFromPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }

    if (showToPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = toMillis)
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { toMillis = it }
                    showToPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showToPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun DateRangeOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) BrandIndigoLight else SurfaceWhite)
            .border(1.dp, if (selected) BrandIndigo else BorderGray, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (selected) BrandIndigo else TextPrimary,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DateChip(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.DateRange, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
