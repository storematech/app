package com.quizmaker.android.ui.learners

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.util.formatShortDate
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

enum class ExportFormat { PDF, CSV }

/**
 * From/to date pickers, defaulting to "this month" (1st of the current month through today) —
 * two single-date pickers rather than Material3's DateRangePicker, which has no precedent in this
 * app and doesn't fit well inside a Dialog. Mirrors ui/tools/rsvp/RsvpEventEditSheet.kt's
 * DatePicker/DatePickerDialog pattern, including reading selectedDateMillis back via UTC (not the
 * device timezone) to avoid the same off-by-one-day shift noted there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDateRangeDialog(
    format: ExportFormat,
    onExport: (fromMillis: Long, toMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val firstOfMonth = remember(today) { LocalDate(today.year, today.month, 1) }
    var fromMillis by remember { mutableStateOf(firstOfMonth.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()) }
    var toMillis by remember { mutableStateOf(today.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()) }
    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceWhite)
                .padding(20.dp)
        ) {
            Text(
                if (format == ExportFormat.PDF) "Export as PDF" else "Export as CSV",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text("Choose the date range to export.", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))

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

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { onExport(fromMillis, toMillis) },
                    enabled = fromMillis <= toMillis,
                    modifier = Modifier.weight(1f)
                ) { Text("Export") }
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
private fun DateChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(AppBackground)
            .border(1.dp, BorderGray, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
