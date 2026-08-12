package com.quizmaker.android.ui.leaderboard

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.PdfRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.StatAmberBg
import com.quizmaker.android.core.theme.StatAmberIcon
import com.quizmaker.android.core.theme.StatBlueBg
import com.quizmaker.android.core.theme.StatBlueIcon
import com.quizmaker.android.core.theme.StatGreenBg
import com.quizmaker.android.core.theme.StatGreenIcon
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.LeaderboardEntry
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.ScorePill
import com.quizmaker.android.ui.common.StatTile
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.LeaderboardPdfExporter
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

private val GoldColor = Color(0xFFF5B700)
private val SilverColor = Color(0xFF94A3B8)
private val BronzeColor = Color(0xFFB45309)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Leaderboard", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        uiState.data?.quizTitle?.let {
                            Text(it, fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    val data = uiState.data
                    if (data != null && data.entries.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch {
                                val branding = viewModel.getPdfBranding()
                                val intent = LeaderboardPdfExporter.export(context, data.quizTitle, data, branding)
                                context.startActivity(Intent.createChooser(intent, "Export leaderboard"))
                            }
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = PdfRed)
                        }
                    }
                }
            )
        }
    ) { padding ->
        LoadingCrossfade(
            isLoading = uiState.isLoading,
            modifier = Modifier.padding(padding),
            loadingContent = { ListScreenSkeleton(statTileCount = 3, rowCount = 6, rowLineWidths = listOf(150.dp, 200.dp)) }
        ) {
            when {
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                    ErrorBanner(message = uiState.errorMessage!!)
                }
                uiState.data != null -> {
                    val data = uiState.data!!
                    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatTile(
                                    icon = Icons.Default.Group,
                                    iconBg = StatBlueBg,
                                    iconTint = StatBlueIcon,
                                    label = "Entries",
                                    value = data.totalAttempts.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                StatTile(
                                    icon = Icons.Default.Assessment,
                                    iconBg = StatAmberBg,
                                    iconTint = StatAmberIcon,
                                    label = "Avg Score",
                                    value = "${data.averageScore}%",
                                    modifier = Modifier.weight(1f)
                                )
                                StatTile(
                                    icon = Icons.Default.Star,
                                    iconBg = StatGreenBg,
                                    iconTint = StatGreenIcon,
                                    label = "Top Score",
                                    value = data.entries.firstOrNull()?.let { "${percentFor(it, data.maxPoints)}%" } ?: "0%",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                        }

                        if (data.entries.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Default.EmojiEvents,
                                    title = "No completed attempts yet",
                                    subtitle = "Once people take this quiz, rankings will show up here."
                                )
                            }
                        } else {
                            items(data.entries, key = { it.rank }) { entry ->
                                LeaderboardRow(entry = entry, maxPoints = data.maxPoints)
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun percentFor(entry: LeaderboardEntry, maxPoints: Int): Int =
    if (maxPoints > 0) (entry.earnedPoints * 100 / maxPoints) else 0

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry, maxPoints: Int) {
    val percent = percentFor(entry, maxPoints)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(18.dp), elevation = 3.dp)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                RankBadge(rank = entry.rank)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(entry.userName, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(entry.userEmail, color = TextSecondary, fontSize = 12.sp)
                }
            }
            ScorePill(percent = percent)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Points: ${entry.earnedPoints}/$maxPoints", color = TextSecondary, fontSize = 12.sp)
            Text("Time: ${formatTimeTaken(entry.timeTakenSeconds)}", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    when (rank) {
        1 -> Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldColor)
        2 -> Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SilverColor)
        3 -> Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = BronzeColor)
        else -> Text("#$rank", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

private fun formatTimeTaken(seconds: Int?): String =
    if (seconds == null) "—" else "${seconds / 60}m ${seconds % 60}s"
