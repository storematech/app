package com.quizmaker.android.ui.tools.voting

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
import com.quizmaker.android.data.model.Candidate
import com.quizmaker.android.data.model.VotingCampaign
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.formatShortDate
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

private const val MAX_CANDIDATES = 12

private fun defaultCandidates(): List<Candidate> = listOf(
    Candidate(id = UUID.randomUUID().toString(), name = ""),
    Candidate(id = UUID.randomUUID().toString(), name = "")
)

/**
 * "New/Edit Voting Campaign" bottom sheet — [initialCampaign] null means creating; non-null pre-fills
 * for editing. Same shape as PollEditSheet. Candidate `bio` isn't exposed here (kept null on create,
 * preserved as-is on edit) — a rarely-needed field for a mobile management screen; add a UI for it
 * later if teachers ask.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingEditSheet(
    initialCampaign: VotingCampaign?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, candidates: List<Candidate>, requireEmail: Boolean, opensAt: String?, closesAt: String?, showResults: Boolean, isActive: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(initialCampaign) { mutableStateOf(initialCampaign?.title.orEmpty()) }
    var description by remember(initialCampaign) { mutableStateOf(initialCampaign?.description.orEmpty()) }
    var candidates by remember(initialCampaign) { mutableStateOf(initialCampaign?.candidates ?: defaultCandidates()) }
    var requireEmail by remember(initialCampaign) { mutableStateOf(initialCampaign?.requireEmail ?: true) }
    var showResults by remember(initialCampaign) { mutableStateOf(initialCampaign?.showResults ?: false) }
    var hasWindow by remember(initialCampaign) { mutableStateOf(initialCampaign?.opensAt != null || initialCampaign?.closesAt != null) }
    var opensAt by remember(initialCampaign) { mutableStateOf(initialCampaign?.opensAt) }
    var closesAt by remember(initialCampaign) { mutableStateOf(initialCampaign?.closesAt) }
    var isActive by remember(initialCampaign) { mutableStateOf(initialCampaign?.isActive ?: true) }
    val isEditing = initialCampaign != null

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
                        if (isEditing) "Edit Voting Campaign" else "New Voting Campaign",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Text(
                        if (isEditing) "Update this campaign" else "Add candidates and share the link",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("CAMPAIGN TITLE", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("e.g. Where should we go for lunch?") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("DESCRIPTION (OPTIONAL)", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
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
            Text("CANDIDATES", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(8.dp))
            candidates.forEachIndexed { index, candidate ->
                CandidateEditorRow(
                    candidate = candidate,
                    canRemove = candidates.size > 2,
                    onUpdate = { updated -> candidates = candidates.toMutableList().also { it[index] = updated } },
                    onRemove = { candidates = candidates.filterIndexed { i, _ -> i != index } }
                )
                Spacer(Modifier.height(8.dp))
            }
            if (candidates.size < MAX_CANDIDATES) {
                Text(
                    "+ Add candidate",
                    color = BrandIndigo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { candidates = candidates + Candidate(id = UUID.randomUUID().toString(), name = "") }
                )
            }

            Spacer(Modifier.height(16.dp))
            ToggleRow(
                title = "Require voter email",
                subtitle = "One ballot per email address.",
                checked = requireEmail,
                onCheckedChange = { requireEmail = it }
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
                title = "Set a voting window",
                subtitle = "Limit voting to a specific date range.",
                checked = hasWindow,
                onCheckedChange = { checked ->
                    hasWindow = checked
                    if (!checked) {
                        opensAt = null
                        closesAt = null
                    }
                }
            )
            if (hasWindow) {
                Spacer(Modifier.height(10.dp))
                Text("OPENS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(6.dp))
                opensAt?.let {
                    Text("Opens ${formatShortDate(it)}", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateChip("Now", onClick = { opensAt = Clock.System.now().toString() })
                    DateChip("Tomorrow", onClick = { opensAt = (Clock.System.now() + 1.days).toString() })
                    DateChip("In 3 Days", onClick = { opensAt = (Clock.System.now() + 3.days).toString() })
                }
                Spacer(Modifier.height(12.dp))
                Text("CLOSES", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                Spacer(Modifier.height(6.dp))
                closesAt?.let {
                    Text("Closes ${formatShortDate(it)}", color = TextSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateChip("1 Day", onClick = { closesAt = (Clock.System.now() + 1.days).toString() })
                    DateChip("3 Days", onClick = { closesAt = (Clock.System.now() + 3.days).toString() })
                    DateChip("7 Days", onClick = { closesAt = (Clock.System.now() + 7.days).toString() })
                    DateChip("30 Days", onClick = { closesAt = (Clock.System.now() + 30.days).toString() })
                }
            }

            Spacer(Modifier.height(12.dp))
            ToggleRow(
                title = "Accepting ballots",
                subtitle = "Turn off to close the campaign.",
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
                        text = if (isEditing) "Save Changes" else "Create Campaign",
                        onClick = { onSave(title, description, candidates, requireEmail, opensAt, closesAt, showResults, isActive) },
                        loading = isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateEditorRow(candidate: Candidate, canRemove: Boolean, onUpdate: (Candidate) -> Unit, onRemove: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(14.dp), elevation = 2.dp)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = candidate.name,
                onValueChange = { onUpdate(candidate.copy(name = it)) },
                placeholder = { Text("e.g. Taco Bell") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove candidate", tint = TextSecondary)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = candidate.role.orEmpty(),
            onValueChange = { onUpdate(candidate.copy(role = it.ifBlank { null })) },
            placeholder = { Text("e.g. Mexican food (optional)") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
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
