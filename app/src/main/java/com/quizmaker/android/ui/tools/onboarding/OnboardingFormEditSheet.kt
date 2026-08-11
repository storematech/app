package com.quizmaker.android.ui.tools.onboarding

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.OnboardingForm
import com.quizmaker.android.data.model.ToolField
import com.quizmaker.android.data.model.ToolFieldType
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.elevatedSurface
import java.util.UUID

private const val MAX_FIELDS = 10

private fun defaultFields(): List<ToolField> = listOf(
    ToolField(id = UUID.randomUUID().toString(), label = "Full name", type = ToolFieldType.SHORT_TEXT, required = true),
    ToolField(id = UUID.randomUUID().toString(), label = "Email address", type = ToolFieldType.EMAIL, required = true),
    ToolField(id = UUID.randomUUID().toString(), label = "Phone number", type = ToolFieldType.PHONE, required = false)
)

/** "New/Edit Onboarding Form" bottom sheet — [initialForm] null means creating; non-null pre-fills for editing. Owns all its own state and hands the finished values back via [onSave]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFormEditSheet(
    initialForm: OnboardingForm?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, welcomeMessage: String, fields: List<ToolField>, isActive: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(initialForm) { mutableStateOf(initialForm?.title.orEmpty()) }
    var description by remember(initialForm) { mutableStateOf(initialForm?.description.orEmpty()) }
    var welcomeMessage by remember(initialForm) { mutableStateOf(initialForm?.welcomeMessage.orEmpty()) }
    var fields by remember(initialForm) { mutableStateOf(initialForm?.fields ?: defaultFields()) }
    var isActive by remember(initialForm) { mutableStateOf(initialForm?.isActive ?: true) }
    val isEditing = initialForm != null

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
                        if (isEditing) "Edit Onboarding Form" else "New Onboarding Form",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Text(
                        if (isEditing) "Update this form" else "Build a form to collect learner details",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("FORM TITLE", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("e.g. New Student Registration") },
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
                placeholder = { Text("What this form is for") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("AFTER SUBMISSION MESSAGE (OPTIONAL)", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = welcomeMessage,
                onValueChange = { welcomeMessage = it },
                placeholder = { Text("Welcome aboard! We'll be in touch shortly.") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            Text("FORM FIELDS", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(10.dp))
            fields.forEachIndexed { index, field ->
                FieldEditorCard(
                    field = field,
                    onUpdate = { updated -> fields = fields.toMutableList().also { it[index] = updated } },
                    onRemove = { fields = fields.filterIndexed { i, _ -> i != index } }
                )
                Spacer(Modifier.height(10.dp))
            }
            if (fields.size < MAX_FIELDS) {
                Text(
                    "+ Add field (${fields.size}/$MAX_FIELDS)",
                    color = BrandIndigo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        fields = fields + ToolField(id = UUID.randomUUID().toString(), label = "", type = ToolFieldType.SHORT_TEXT, required = false)
                    }
                )
            } else {
                Text("Maximum $MAX_FIELDS fields allowed.", color = TextSecondary, fontSize = 12.sp)
            }

            Spacer(Modifier.height(20.dp))
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
                    Text("Accepting submissions", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Turn off to close the form.", color = TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandIndigo)
                )
            }

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
                        text = if (isEditing) "Save Changes" else "Create Form",
                        onClick = { onSave(title, description, welcomeMessage, fields, isActive) },
                        loading = isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldEditorCard(field: ToolField, onUpdate: (ToolField) -> Unit, onRemove: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 2.dp)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = field.label,
                onValueChange = { onUpdate(field.copy(label = it)) },
                placeholder = { Text("Field label") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove field", tint = ErrorRed)
            }
        }
        Spacer(Modifier.height(8.dp))
        FieldTypeDropdown(
            selected = field.type,
            onSelect = { newType ->
                onUpdate(
                    field.copy(
                        type = newType,
                        options = if (newType.needsOptions) field.options?.takeIf { it.isNotEmpty() } ?: listOf("Option 1", "Option 2") else null
                    )
                )
            }
        )
        if (field.type.needsOptions) {
            Spacer(Modifier.height(10.dp))
            Text("CHOICES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(6.dp))
            val options = field.options.orEmpty()
            options.forEachIndexed { index, option ->
                Row(modifier = Modifier.padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = option,
                        onValueChange = { text -> onUpdate(field.copy(options = options.toMutableList().also { it[index] = text })) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onUpdate(field.copy(options = options.filterIndexed { i, _ -> i != index })) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove choice", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Text(
                "+ Add choice",
                color = BrandIndigo,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onUpdate(field.copy(options = options + "Option ${options.size + 1}")) }
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Required", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = field.required,
                onCheckedChange = { onUpdate(field.copy(required = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandIndigo)
            )
        }
    }
}

/** Plain Box + DropdownMenu instead of ExposedDropdownMenuBox — avoids that API's version churn, and this is a simple "pick one of ten" control, not a text-editable field. */
@Composable
private fun FieldTypeDropdown(selected: ToolFieldType, onSelect: (ToolFieldType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selected.label, color = TextPrimary, fontSize = 14.sp)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ToolFieldType.entries.forEach { type ->
                DropdownMenuItem(text = { Text(type.label) }, onClick = { onSelect(type); expanded = false })
            }
        }
    }
}
