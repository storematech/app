package com.quizmaker.android.ui.tools.rsvp

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.RsvpEvent
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.util.formatDateTime
import com.quizmaker.android.util.formatShortDate
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** "New/Edit Event" bottom sheet — [initialEvent] null means creating; non-null pre-fills for editing. Same shape as PollEditSheet/VotingEditSheet, but flatter — no dynamic field/candidate array for this tool. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RsvpEventEditSheet(
    initialEvent: RsvpEvent?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, location: String, eventDate: String?, capacity: Int?, rsvpDeadline: String?, allowGuests: Boolean, isActive: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(initialEvent) { mutableStateOf(initialEvent?.title.orEmpty()) }
    var description by remember(initialEvent) { mutableStateOf(initialEvent?.description.orEmpty()) }
    var location by remember(initialEvent) { mutableStateOf(initialEvent?.location.orEmpty()) }
    var eventDate by remember(initialEvent) { mutableStateOf(initialEvent?.eventDate) }
    var capacityText by remember(initialEvent) { mutableStateOf(initialEvent?.capacity?.toString().orEmpty()) }
    var hasDeadline by remember(initialEvent) { mutableStateOf(initialEvent?.rsvpDeadline != null) }
    var rsvpDeadline by remember(initialEvent) { mutableStateOf(initialEvent?.rsvpDeadline) }
    var allowGuests by remember(initialEvent) { mutableStateOf(initialEvent?.allowGuests ?: false) }
    var isActive by remember(initialEvent) { mutableStateOf(initialEvent?.isActive ?: true) }
    val isEditing = initialEvent != null

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
                        if (isEditing) "Edit Event" else "New Event",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Text(
                        if (isEditing) "Update this event" else "Set up an event and share the link",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("EVENT TITLE", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("e.g. Annual Sports Day") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("MESSAGE (OPTIONAL)", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Any extra details for guests") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("LOCATION (OPTIONAL)", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                placeholder = { Text("e.g. Main Auditorium") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("EVENT DATE", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            EventDateTimePicker(isoValue = eventDate, onChange = { eventDate = it })

            Spacer(Modifier.height(16.dp))
            Text("CAPACITY (OPTIONAL)", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = capacityText,
                onValueChange = { text -> capacityText = text.filter { it.isDigit() } },
                placeholder = { Text("Max attendees, leave blank for unlimited") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            ToggleRow(
                title = "Allow guests",
                subtitle = "Let attendees bring additional people.",
                checked = allowGuests,
                onCheckedChange = { allowGuests = it }
            )

            Spacer(Modifier.height(12.dp))
            ToggleRow(
                title = "Set an RSVP deadline",
                subtitle = "Stop accepting registrations after a date.",
                checked = hasDeadline,
                onCheckedChange = { checked ->
                    hasDeadline = checked
                    if (!checked) rsvpDeadline = null
                }
            )
            if (hasDeadline) {
                Spacer(Modifier.height(10.dp))
                rsvpDeadline?.let {
                    Text("Deadline ${formatShortDate(it)}", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateChip("1 Day", onClick = { rsvpDeadline = (Clock.System.now() + 1.days).toString() })
                    DateChip("3 Days", onClick = { rsvpDeadline = (Clock.System.now() + 3.days).toString() })
                    DateChip("7 Days", onClick = { rsvpDeadline = (Clock.System.now() + 7.days).toString() })
                }
            }

            Spacer(Modifier.height(12.dp))
            ToggleRow(
                title = "Accepting registrations",
                subtitle = "Turn off to close the event.",
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
                        text = if (isEditing) "Save Changes" else "Create Event",
                        onClick = {
                            onSave(title, description, location, eventDate, capacityText.toIntOrNull(), rsvpDeadline, allowGuests, isActive)
                        },
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
private fun DateChip(label: String, onClick: () -> Unit) {
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

/**
 * Calendar date picker + a separate "+ Add Time" step, since an event may only have a date and no
 * specific time yet. Picking a date alone stores midnight local time; tapping "+ Add Time" opens a
 * time picker that keeps the already-picked date and only changes the time-of-day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDateTimePicker(isoValue: String?, onChange: (String?) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val currentLocalDateTime = isoValue
        ?.let { runCatching { Instant.parse(it) }.getOrNull() }
        ?.toLocalDateTime(TimeZone.currentSystemDefault())

    Column {
        currentLocalDateTime?.let {
            Text(formatDateTime(isoValue), color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateChip(if (currentLocalDateTime == null) "Pick Date" else "Change Date") { showDatePicker = true }
            if (currentLocalDateTime != null) {
                DateChip("+ Add Time") { showTimePicker = true }
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
                        // DatePicker's millis are UTC-anchored to the picked calendar date — reading it back
                        // via UTC (not the device timezone) is what avoids an off-by-one-day shift here.
                        val pickedDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
                        val hour = currentLocalDateTime?.hour ?: 0
                        val minute = currentLocalDateTime?.minute ?: 0
                        val instant = pickedDate.atTime(hour, minute).toInstant(TimeZone.currentSystemDefault())
                        onChange(instant.toString())
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
        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                val instant = currentLocalDateTime.date
                    .atTime(timePickerState.hour, timePickerState.minute)
                    .toInstant(TimeZone.currentSystemDefault())
                onChange(instant.toString())
                showTimePicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

/** Material3 ships DatePickerDialog but not an equivalent for TimePicker — this wraps it the same way. */
@Composable
private fun TimePickerDialog(onDismiss: () -> Unit, onConfirm: () -> Unit, content: @Composable () -> Unit) {
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
