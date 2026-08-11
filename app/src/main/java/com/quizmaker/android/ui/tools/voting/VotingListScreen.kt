package com.quizmaker.android.ui.tools.voting

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.VotingCampaign
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.OutlinedPill
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.QrCodeGenerator
import com.quizmaker.android.util.formatShortDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingListScreen(
    onNavigateBack: () -> Unit,
    onViewResults: (String) -> Unit,
    viewModel: VotingListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var campaignPendingShare by remember { mutableStateOf<VotingCampaign?>(null) }
    var campaignPendingMenu by remember { mutableStateOf<VotingCampaign?>(null) }
    var campaignPendingDelete by remember { mutableStateOf<VotingCampaign?>(null) }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text("Voting", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
                GradientButton(
                    text = "Create Voting Campaign",
                    onClick = viewModel::openCreateSheet,
                    leadingIcon = Icons.Default.NoteAdd,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                uiState.errorMessage?.let {
                    ErrorBanner(message = it, onRetry = viewModel::refresh)
                    Spacer(Modifier.height(12.dp))
                }
            }

            LoadingCrossfade(
                isLoading = uiState.isLoading,
                loadingContent = { ListScreenSkeleton(rowCount = 4, rowLineWidths = listOf(180.dp, 220.dp)) }
            ) {
                if (uiState.campaigns.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        EmptyState(
                            icon = Icons.Default.CheckCircle,
                            title = "No voting campaigns yet",
                            subtitle = "Set up candidates and share the link to collect ballots."
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(uiState.campaigns, key = { it.id }) { campaign ->
                            VotingCampaignCard(
                                campaign = campaign,
                                voteCount = uiState.voteCounts[campaign.id] ?: 0,
                                onEdit = { viewModel.openEditSheet(campaign) },
                                onShare = { campaignPendingShare = campaign },
                                onViewResults = { onViewResults(campaign.id) },
                                onMenuClick = { campaignPendingMenu = campaign }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    if (uiState.isEditSheetOpen) {
        VotingEditSheet(
            initialCampaign = uiState.editingCampaign,
            isSaving = uiState.isSaving,
            onDismiss = viewModel::dismissEditSheet,
            onSave = viewModel::saveCampaign
        )
    }

    campaignPendingShare?.let { campaign ->
        VotingShareSheet(title = campaign.title, shareUrl = campaign.shareUrl, onDismiss = { campaignPendingShare = null })
    }

    campaignPendingMenu?.let { campaign ->
        VotingActionsSheet(
            campaign = campaign,
            onDismiss = { campaignPendingMenu = null },
            onToggleActive = { viewModel.toggleActive(campaign) },
            onRequestDelete = { campaignPendingDelete = campaign }
        )
    }

    campaignPendingDelete?.let { campaign ->
        AlertDialog(
            onDismissRequest = { campaignPendingDelete = null },
            title = { Text("Delete \"${campaign.title}\"?") },
            text = { Text("This permanently deletes the campaign and all of its ballots.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCampaign(campaign.id)
                    campaignPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { campaignPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun VotingCampaignCard(
    campaign: VotingCampaign,
    voteCount: Int,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onViewResults: () -> Unit,
    onMenuClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onViewResults)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(campaign.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary, modifier = Modifier.weight(1f))
            OutlinedPill(
                text = if (campaign.isActive) "Active" else "Inactive",
                borderColor = if (campaign.isActive) BrandIndigo else BorderGray,
                contentColor = if (campaign.isActive) BrandIndigo else TextSecondary
            )
        }
        if (!campaign.description.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(campaign.description, color = TextSecondary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("${campaign.candidates.size} candidates", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        if (campaign.opensAt != null || campaign.closesAt != null) {
            Spacer(Modifier.height(4.dp))
            val windowText = buildString {
                campaign.opensAt?.let { append("Opens ${formatShortDate(it)}") }
                if (campaign.opensAt != null && campaign.closesAt != null) append("  ·  ")
                campaign.closesAt?.let { append("Closes ${formatShortDate(it)}") }
            }
            Text(windowText, color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                campaign.shareUrl,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy link",
                tint = BrandIndigo,
                modifier = Modifier.size(16.dp).clickable { clipboardManager.setText(AnnotatedString(campaign.shareUrl)) }
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("$voteCount ballots cast", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircleIconButton(icon = Icons.Default.Edit, onClick = onEdit)
            CircleIconButton(icon = Icons.Default.Share, onClick = onShare)
            CircleIconButton(icon = Icons.Default.BarChart, onClick = onViewResults)
            CircleIconButton(icon = Icons.Default.MoreVert, onClick = onMenuClick)
        }
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(1.dp, BorderGray, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
    }
}

/** Same screen-covering, Dialog-based actions panel pattern as QuizActionsSheet / OnboardingFormActionsSheet / PollActionsSheet. */
@Composable
private fun VotingActionsSheet(
    campaign: VotingCampaign,
    onDismiss: () -> Unit,
    onToggleActive: () -> Unit,
    onRequestDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }
        val noRipple = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(indication = null, interactionSource = noRipple, onClick = onDismiss)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(SurfaceWhite)
                        .clickable(indication = null, interactionSource = noRipple, onClick = {})
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                ) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(BorderGray)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        campaign.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(8.dp))

                    SheetActionRow(
                        label = if (campaign.isActive) "Deactivate Campaign" else "Activate Campaign",
                        icon = if (campaign.isActive) Icons.Default.Lock else Icons.Default.LockOpen
                    ) { onDismiss(); onToggleActive() }
                    SheetActionRow("Delete Campaign", Icons.Default.Delete, tint = ErrorRed) { onDismiss(); onRequestDelete() }
                }
            }
        }
    }
}

@Composable
private fun SheetActionRow(label: String, icon: ImageVector, tint: Color = TextPrimary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

/** Minimal share sheet — full link + copy + QR + native share, no "share code" card (a slug isn't a code meant to be read aloud). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VotingShareSheet(title: String, shareUrl: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showQr by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(BrandIndigoLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Share Campaign", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                    Text(title, color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(shareUrl, color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = BrandIndigo,
                    modifier = Modifier.size(18.dp).clickable { clipboardManager.setText(AnnotatedString(shareUrl)) }
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.QrCode2,
                    contentDescription = "Show QR code",
                    tint = BrandIndigo,
                    modifier = Modifier.size(18.dp).clickable { showQr = !showQr }
                )
            }

            if (showQr) {
                Spacer(Modifier.height(16.dp))
                val qrBitmap = remember(shareUrl) { QrCodeGenerator.generate(shareUrl) }
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR code linking to this campaign",
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            GradientButton(
                text = "Share Link",
                leadingIcon = Icons.Default.Share,
                onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Cast your vote in \"$title\": $shareUrl")
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share campaign"))
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SurfaceWhite)
                    .border(1.dp, BorderGray, RoundedCornerShape(50))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text("Done", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
