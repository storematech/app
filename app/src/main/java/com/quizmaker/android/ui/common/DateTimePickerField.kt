package com.quizmaker.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.util.formatDateTime
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Calendar date picker + a separate "+ Add Time" step, operating directly on [Instant] rather than
 * an ISO string — extracted from RsvpEventEditSheet's own `EventDateTimePicker` (same UTC-anchored
 * DatePicker-millis gotcha applies here, see the inline comment below) so quiz scheduling doesn't
 * re-derive that logic. [onChange] receives null when "Clear" is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerField(label: String, value: Instant?, onChange: (Instant?) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val currentLocalDateTime = value?.toLocalDateTime(TimeZone.currentSystemDefault())

    Column {
        Text(label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        currentLocalDateTime?.let {
            Text(formatDateTime(value), color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PickerChip(if (currentLocalDateTime == null) "Pick date" else "Change date") { showDatePicker = true }
            if (currentLocalDateTime != null) {
                PickerChip("Change time") { showTimePicker = true }
                PickerChip("Clear") { onChange(null) }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentLocalDateTime?.date
                ?.atTime(0, 0)
                ?.toInstant(TimeZone.UTC)
                ?.toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        // DatePicker's millis are UTC-anchored to the picked calendar date — reading it
                        // back via UTC (not the device timezone) is what avoids an off-by-one-day shift.
                        val pickedDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                        val hour = currentLocalDateTime?.hour ?: 0
                        val minute = currentLocalDateTime?.minute ?: 0
                        onChange(pickedDate.atTime(hour, minute).toInstant(TimeZone.currentSystemDefault()))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker && currentLocalDateTime != null) {
        val timePickerState = rememberTimePickerState(
            initialHour = currentLocalDateTime.hour,
            initialMinute = currentLocalDateTime.minute
        )
        DateTimePickerFieldTimeDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                val instant = currentLocalDateTime.date
                    .atTime(timePickerState.hour, timePickerState.minute)
                    .toInstant(TimeZone.currentSystemDefault())
                onChange(instant)
                showTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
private fun PickerChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(AppBackground, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** Material3 ships DatePickerDialog but not an equivalent for TimePicker — same wrapper approach as
 *  RsvpEventEditSheet's own TimePickerDialog, named distinctly since both are file-private. */
@Composable
private fun DateTimePickerFieldTimeDialog(onDismiss: () -> Unit, onConfirm: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = SurfaceWhite) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                content()
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}
