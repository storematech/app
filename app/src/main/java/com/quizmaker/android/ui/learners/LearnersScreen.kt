package com.quizmaker.android.ui.learners

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.Learner
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.OutlinedPill
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.GenericCsvExporter
import com.quizmaker.android.util.GenericTablePdfExporter
import com.quizmaker.android.util.formatShortDate
import kotlin.time.Instant
import kotlinx.coroutines.launch
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnersScreen(
    onNavigateBack: () -> Unit,
    viewModel: LearnersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text("Learners", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedPillButton(
                        text = "Create Group",
                        icon = Icons.Default.GroupAdd,
                        onClick = viewModel::openCreateGroupDialog,
                        modifier = Modifier.weight(1f)
                    )
                    GradientButton(
                        text = "Add Learner",
                        onClick = viewModel::openCreateLearnerDialog,
                        leadingIcon = Icons.Default.PersonAdd,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(16.dp))

                var showAutoCreateInfo by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .elevatedSurface(shape = RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto Create Learners", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    IconButton(onClick = { showAutoCreateInfo = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Info, contentDescription = "What is Auto Create?", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = uiState.autoCreateEnabled,
                        onCheckedChange = viewModel::onAutoCreateToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandIndigo)
                    )
                }
                if (showAutoCreateInfo) {
                    AlertDialog(
                        onDismissRequest = { showAutoCreateInfo = false },
                        title = { Text("Auto Create Learners") },
                        text = { Text("When learners submit a quiz, we'll automatically create a learner profile for them using their name and email.") },
                        confirmButton = {
                            TextButton(onClick = { showAutoCreateInfo = false }) { Text("Got it") }
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${uiState.filteredLearners.size} of ${uiState.learners.size} learners",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    if (uiState.searchQuery.isNotBlank() || uiState.groupFilter != "all") {
                        Text(
                            "Clear filters",
                            color = BrandIndigo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable(onClick = viewModel::clearFilters)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("Search by name, ID, or email...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        GroupFilterChip(label = "All Groups", selected = uiState.groupFilter == "all") {
                            viewModel.onGroupFilterChange("all")
                        }
                    }
                    item {
                        GroupFilterChip(label = "No Group", selected = uiState.groupFilter == "no-group") {
                            viewModel.onGroupFilterChange("no-group")
                        }
                    }
                    items(uiState.groups, key = { it.id }) { group ->
                        GroupFilterChip(label = group.name, selected = uiState.groupFilter == group.id) {
                            viewModel.onGroupFilterChange(group.id)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                uiState.errorMessage?.let {
                    ErrorBanner(message = it, onRetry = viewModel::refresh)
                    Spacer(Modifier.height(12.dp))
                }
            }

            LoadingCrossfade(
                isLoading = uiState.isLoading,
                loadingContent = { ListScreenSkeleton(rowCount = 6, rowLineWidths = listOf(160.dp, 110.dp)) }
            ) {
                if (uiState.filteredLearners.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        EmptyState(
                            icon = Icons.Default.Group,
                            title = if (uiState.learners.isEmpty()) "No learners yet" else "No matches",
                            subtitle = if (uiState.learners.isEmpty()) {
                                "Add your first learner to start tracking their progress."
                            } else {
                                "Try a different search or filter."
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.paginatedLearners, key = { it.id }) { learner ->
                            LearnerCard(
                                learner = learner,
                                onView = { viewModel.openStudentProfile(learner) },
                                onEdit = { viewModel.openEditLearnerDialog(learner) },
                                onDeleteRequest = { viewModel.requestDeleteLearner(learner) }
                            )
                        }
                        if (uiState.totalPages > 1) {
                            item {
                                Spacer(Modifier.height(4.dp))
                                PaginationControls(
                                    currentPage = uiState.currentPage,
                                    totalPages = uiState.totalPages,
                                    onPageChange = viewModel::setPage
                                )
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    uiState.learnerToDelete?.let { learner ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteLearner,
            title = { Text("Delete Learner") },
            text = { Text("Are you sure you want to delete ${learner.name}? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteLearner, enabled = !uiState.isDeletingLearner) {
                    Text(if (uiState.isDeletingLearner) "Deleting…" else "Delete", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteLearner) { Text("Cancel") }
            }
        )
    }

    if (uiState.isLearnerFormOpen) {
        LearnerFormDialog(
            editingLearner = uiState.editingLearner,
            groups = uiState.groups,
            isSaving = uiState.isSavingLearner,
            onDismiss = viewModel::dismissLearnerForm,
            onSave = viewModel::saveLearner,
            onQuickCreateGroup = viewModel::quickCreateGroup
        )
    }

    if (uiState.isCreateGroupDialogOpen) {
        CreateGroupDialog(
            isSaving = uiState.isSavingGroup,
            onDismiss = viewModel::dismissCreateGroupDialog,
            onCreate = viewModel::createGroup
        )
    }

    val profileLearner = uiState.profileLearner
    if (uiState.isStudentProfileOpen && profileLearner != null) {
        StudentProfileDialog(
            learner = profileLearner,
            attempts = uiState.profileAttempts,
            isLoadingAttempts = uiState.isLoadingProfileAttempts,
            onExportRequest = viewModel::requestExport,
            onDismiss = viewModel::dismissStudentProfile
        )
    }

    val pendingFormat = uiState.pendingExportFormat
    if (pendingFormat != null && profileLearner != null) {
        ExportDateRangeDialog(
            format = pendingFormat,
            onDismiss = viewModel::dismissExportDialog,
            onExport = { fromMillis, toMillis ->
                scope.launch {
                    val filtered = uiState.profileAttempts.filter { attempt ->
                        val completedMillis = attempt.completedAt
                            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
                            ?.toEpochMilliseconds()
                        completedMillis != null && completedMillis in fromMillis..toMillis
                    }
                    val columns = listOf("Quiz" to 180f, "Score" to 60f, "Time Taken" to 80f, "Completed" to 120f)
                    val rows = filtered.map { attempt ->
                        listOf(
                            attempt.quizTitle,
                            attempt.score?.let { "$it%" } ?: "-",
                            attempt.timeTaken?.let { "${it / 60}m ${it % 60}s" } ?: "-",
                            formatShortDate(attempt.completedAt)
                        )
                    }
                    val safeName = profileLearner.name.ifBlank { "learner" }.replace(Regex("[^A-Za-z0-9]+"), "_")
                    val intent = when (pendingFormat) {
                        ExportFormat.CSV -> GenericCsvExporter.export(context, "${safeName}_attempts.csv", columns.map { it.first }, rows)
                        ExportFormat.PDF -> GenericTablePdfExporter.export(
                            context = context,
                            fileName = "${safeName}_attempts.pdf",
                            title = "${profileLearner.name} — Quiz Attempts",
                            columns = columns,
                            rows = rows,
                            branding = viewModel.getPdfBranding()
                        )
                    }
                    context.startActivity(Intent.createChooser(intent, "Export"))
                    viewModel.dismissExportDialog()
                }
            }
        )
    }
}

@Composable
private fun LearnerCard(
    learner: Learner,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    learner.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                learner.groupName?.let {
                    Spacer(Modifier.width(8.dp))
                    OutlinedPill(text = it, borderColor = BrandIndigo, contentColor = BrandIndigo)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("ID: ${learner.studentId}", color = TextSecondary, fontSize = 12.sp)
            if (!learner.email.isNullOrBlank()) {
                Text(learner.email, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = onView) { Icon(Icons.Default.Visibility, contentDescription = "View profile", tint = TextSecondary) }
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary) }
        IconButton(onClick = onDeleteRequest) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed) }
    }
}

@Composable
private fun GroupFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) BrandIndigo else AppBackground)
            .border(1.dp, if (selected) BrandIndigo else BorderGray, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun OutlinedPillButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, BorderGray, RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PaginationControls(currentPage: Int, totalPages: Int, onPageChange: (Int) -> Unit) {
    val windowSize = 5
    val pages = when {
        totalPages <= windowSize -> 1..totalPages
        currentPage <= 3 -> 1..windowSize
        currentPage >= totalPages - 2 -> (totalPages - windowSize + 1)..totalPages
        else -> (currentPage - 2)..(currentPage + 2)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onPageChange(currentPage - 1) }, enabled = currentPage > 1) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous page")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            pages.forEach { page ->
                val selected = page == currentPage
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) BrandIndigo else Color.Transparent)
                        .border(1.dp, if (selected) BrandIndigo else BorderGray, RoundedCornerShape(10.dp))
                        .clickable { onPageChange(page) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$page",
                        color = if (selected) Color.White else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        IconButton(onClick = { onPageChange(currentPage + 1) }, enabled = currentPage < totalPages) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next page")
        }
    }
}
