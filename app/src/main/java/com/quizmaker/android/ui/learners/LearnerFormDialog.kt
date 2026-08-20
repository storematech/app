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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.Group
import com.quizmaker.android.data.model.Learner
import com.quizmaker.android.ui.common.GradientButton
import kotlinx.coroutines.launch

private const val NO_GROUP_ID = "no-group"

/** Create/Edit Learner bottom sheet — [editingLearner] null means creating. Mirrors the web's CreateLearnerDialog/EditLearnerDialog, minus Mini LMS course assignment and the learner-login-portal credentials flow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnerFormDialog(
    editingLearner: Learner?,
    groups: List<Group>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, groupId: String?, parentName: String, parentContact: String, notes: String) -> Unit,
    onQuickCreateGroup: suspend (String) -> String?
) {
    // confirmValueChange rejects any auto-driven transition to Hidden (e.g. the sheet's anchors
    // recalculating when the keyboard closes after tapping Create/Save, which otherwise silently
    // dismisses the form — including right when a validation error fires). Explicit dismissal
    // (X, Cancel, a successful save) always goes through onDismiss directly instead, which unmounts
    // this composable from LearnersScreen — so it's unaffected by this and still works normally.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
    val scope = rememberCoroutineScope()
    val isEditing = editingLearner != null

    var name by remember(editingLearner) { mutableStateOf(editingLearner?.name.orEmpty()) }
    var email by remember(editingLearner) { mutableStateOf(editingLearner?.email.orEmpty()) }
    var groupId by remember(editingLearner) { mutableStateOf(editingLearner?.groupId ?: NO_GROUP_ID) }
    var parentName by remember(editingLearner) { mutableStateOf(editingLearner?.parentName.orEmpty()) }
    var parentContact by remember(editingLearner) { mutableStateOf(editingLearner?.parentContact.orEmpty()) }
    var notes by remember(editingLearner) { mutableStateOf(editingLearner?.notes.orEmpty()) }

    var localGroups by remember(editingLearner) { mutableStateOf(groups) }
    var showNewGroup by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var creatingGroup by remember { mutableStateOf(false) }

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
                        if (isEditing) "Edit Learner" else "Create New Learner",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Text(
                        if (isEditing) "Update this learner's details" else "Add a student to your roster",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("NAME", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Full name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("EMAIL", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("student@email.com") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("GROUP/BATCH", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Text(
                    "+ Create Group",
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { showNewGroup = !showNewGroup }
                )
            }
            Spacer(Modifier.height(8.dp))
            if (showNewGroup) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        placeholder = { Text("New group name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (creatingGroup || newGroupName.isBlank()) BorderGray else BrandIndigo)
                            .clickable(enabled = !creatingGroup && newGroupName.isNotBlank()) {
                                scope.launch {
                                    creatingGroup = true
                                    val newId = onQuickCreateGroup(newGroupName)
                                    if (newId != null) {
                                        localGroups = listOf(Group(id = newId, name = newGroupName.trim(), description = null, createdAt = null)) + localGroups
                                        groupId = newId
                                        newGroupName = ""
                                        showNewGroup = false
                                    }
                                    creatingGroup = false
                                }
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (creatingGroup) "…" else "Add", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            GroupDropdown(
                groups = localGroups,
                selectedGroupId = groupId,
                onSelect = { groupId = it }
            )

            Spacer(Modifier.height(16.dp))
            Text("PARENT NAME", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = parentName,
                onValueChange = { parentName = it },
                placeholder = { Text("Parent name (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("PARENT CONTACT", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = parentContact,
                onValueChange = { parentContact = it },
                placeholder = { Text("Phone or email (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("NOTES", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Additional notes about the learner (optional)") },
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
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
                        text = if (isEditing) "Save Changes" else "Create Learner",
                        onClick = {
                            onSave(
                                name,
                                email,
                                groupId.takeIf { it != NO_GROUP_ID },
                                parentName,
                                parentContact,
                                notes
                            )
                        },
                        loading = isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** Plain Box + DropdownMenu (not ExposedDropdownMenuBox — see OnboardingFormEditSheet.kt for why). */
@Composable
private fun GroupDropdown(groups: List<Group>, selectedGroupId: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = if (selectedGroupId == NO_GROUP_ID) "No group" else groups.find { it.id == selectedGroupId }?.name ?: "No group"

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selectedLabel, color = TextPrimary, fontSize = 14.sp)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("No group") }, onClick = { onSelect(NO_GROUP_ID); expanded = false })
            groups.forEach { group ->
                DropdownMenuItem(text = { Text(group.name) }, onClick = { onSelect(group.id); expanded = false })
            }
        }
    }
}
