package com.quizmaker.android.ui.tools.voting

import android.content.Intent
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PdfRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.CandidateTally
import com.quizmaker.android.data.model.VotingBallot
import com.quizmaker.android.ui.common.CsvFileIcon
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.GenericCsvExporter
import com.quizmaker.android.util.GenericTablePdfExporter
import com.quizmaker.android.util.formatShortDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingResultsScreen(
    onNavigateBack: () -> Unit,
    viewModel: VotingResultsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Voting Results",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
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
                    if (uiState.tallies.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch {
                                val branding = viewModel.getPdfBranding()
                                val intent = GenericTablePdfExporter.export(
                                    context = context,
                                    fileName = "voting_results.pdf",
                                    title = uiState.campaign?.title ?: "Voting Results",
                                    columns = listOf("Candidate" to 200f, "Role" to 130f, "Votes" to 80f, "Percentage" to 90f),
                                    rows = uiState.tallies.map { candidateTallyRow(it) },
                                    branding = branding
                                )
                                context.startActivity(Intent.createChooser(intent, "Export results (PDF)"))
                            }
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = PdfRed)
                        }
                        IconButton(onClick = {
                            val intent = GenericCsvExporter.export(
                                context = context,
                                fileName = "voting_results.csv",
                                headers = listOf("Candidate", "Role", "Votes", "Percentage"),
                                rows = uiState.tallies.map { candidateTallyRow(it) }
                            )
                            context.startActivity(Intent.createChooser(intent, "Export results (CSV)"))
                        }) {
                            CsvFileIcon(contentDescription = "Export CSV")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LoadingCrossfade(
            isLoading = uiState.isLoading,
            modifier = Modifier.padding(padding),
            loadingContent = { ListScreenSkeleton(rowCount = 5, rowLineWidths = listOf(200.dp, 140.dp)) }
        ) {
            when {
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                    ErrorBanner(message = uiState.errorMessage!!)
                }
                uiState.campaign == null -> Box(Modifier.fillMaxSize()) {
                    EmptyState(icon = Icons.Default.CheckCircle, title = "Campaign not found")
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column {
                            Text(uiState.campaign!!.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("${uiState.totalBallots} total ballots", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                    items(uiState.tallies, key = { it.candidate.id }) { tally ->
                        CandidateTallyRow(tally)
                    }
                    if (uiState.hasIdentifiedVoters) {
                        item {
                            Spacer(Modifier.height(6.dp))
                            Text("BALLOTS", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                        }
                        val candidateNames = uiState.campaign!!.candidates.associate { it.id to it.name }
                        items(uiState.ballots, key = { it.id }) { ballot ->
                            BallotRow(ballot = ballot, candidateNames = candidateNames)
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CandidateTallyRow(tally: CandidateTally) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(14.dp), elevation = 2.dp)
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tally.candidate.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                tally.candidate.role?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = TextSecondary, fontSize = 12.sp)
                }
            }
            Text("${tally.voteCount} · ${tally.percentage}%", color = TextSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { tally.percentage / 100f },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            color = BrandIndigo,
            trackColor = BorderGray
        )
    }
}

@Composable
private fun BallotRow(ballot: VotingBallot, candidateNames: Map<String, String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(14.dp), elevation = 2.dp)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val headline = ballot.voterName?.takeIf { it.isNotBlank() } ?: ballot.voterEmail ?: "Anonymous"
            Text(headline, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val choice = candidateNames[ballot.candidateId]
            if (!choice.isNullOrBlank()) {
                Text("Voted for $choice", color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(formatShortDate(ballot.createdAt), color = TextSecondary, fontSize = 11.sp)
    }
}

/** Exports the candidate tallies only — the individual ballot list is secondary detail, and often empty for anonymous campaigns. */
private fun candidateTallyRow(tally: CandidateTally): List<String> =
    listOf(tally.candidate.name, tally.candidate.role.orEmpty(), tally.voteCount.toString(), "${tally.percentage}%")
