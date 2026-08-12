package com.quizmaker.android.ui.tools.rsvp

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BrandIndigo
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
import com.quizmaker.android.data.model.AttendingStatus
import com.quizmaker.android.data.model.RsvpRegistration
import com.quizmaker.android.ui.common.CsvFileIcon
import com.quizmaker.android.ui.common.EmptyState
import com.quizmaker.android.ui.common.ErrorBanner
import com.quizmaker.android.ui.common.ListScreenSkeleton
import com.quizmaker.android.ui.common.LoadingCrossfade
import com.quizmaker.android.ui.common.OutlinedPill
import com.quizmaker.android.ui.common.StatTile
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.util.GenericCsvExporter
import com.quizmaker.android.util.GenericTablePdfExporter
import com.quizmaker.android.util.formatDateTime
import com.quizmaker.android.util.formatShortDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RsvpRegistrationsScreen(
    onNavigateBack: () -> Unit,
    viewModel: RsvpRegistrationsViewModel = hiltViewModel()
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
                        uiState.event?.title?.let { "Registrations — $it" } ?: "Registrations",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    if (uiState.registrations.isNotEmpty()) {
                        IconButton(onClick = {
                            scope.launch {
                                val branding = viewModel.getPdfBranding()
                                val intent = GenericTablePdfExporter.export(
                                    context = context,
                                    fileName = "rsvp_registrations.pdf",
                                    title = uiState.event?.title ?: "Registrations",
                                    columns = listOf(
                                        "Name" to 90f, "Email" to 100f, "Phone" to 70f,
                                        "Attending" to 65f, "Guests" to 45f, "Registered" to 90f
                                    ),
                                    rows = uiState.registrations.map { registrationRow(it) },
                                    branding = branding
                                )
                                context.startActivity(Intent.createChooser(intent, "Export registrations (PDF)"))
                            }
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = PdfRed)
                        }
                        IconButton(onClick = {
                            val intent = GenericCsvExporter.export(
                                context = context,
                                fileName = "rsvp_registrations.csv",
                                headers = listOf("Name", "Email", "Phone", "Attending", "Guests", "Notes", "Registered At"),
                                rows = uiState.registrations.map { registrationCsvRow(it) }
                            )
                            context.startActivity(Intent.createChooser(intent, "Export registrations (CSV)"))
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
            loadingContent = { ListScreenSkeleton(rowCount = 6, rowLineWidths = listOf(200.dp, 140.dp)) }
        ) {
            when {
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize().padding(16.dp)) {
                    ErrorBanner(message = uiState.errorMessage!!)
                }
                uiState.registrations.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    EmptyState(
                        icon = Icons.Default.CalendarToday,
                        title = "No registrations yet",
                        subtitle = "Once someone RSVPs, they'll show up here."
                    )
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        val summary = uiState.summary
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StatTile(
                                icon = Icons.Default.Group,
                                iconBg = StatBlueBg,
                                iconTint = StatBlueIcon,
                                label = "Registered",
                                value = (summary?.totalRegistrations ?: 0).toString(),
                                modifier = Modifier.weight(1f)
                            )
                            StatTile(
                                icon = Icons.Default.TrendingUp,
                                iconBg = StatGreenBg,
                                iconTint = StatGreenIcon,
                                label = "Attending",
                                value = (summary?.totalAttending ?: 0).toString(),
                                modifier = Modifier.weight(1f)
                            )
                            if (summary?.capacityRemaining != null) {
                                StatTile(
                                    icon = Icons.Default.CalendarToday,
                                    iconBg = StatAmberBg,
                                    iconTint = StatAmberIcon,
                                    label = "Spots Left",
                                    value = summary.capacityRemaining.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    items(uiState.registrations, key = { it.id }) { registration ->
                        RegistrationRow(registration)
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RegistrationRow(registration: RsvpRegistration) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(16.dp), elevation = 3.dp)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(registration.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            OutlinedPill(
                text = registration.attending.label,
                borderColor = if (registration.attending == AttendingStatus.YES) BrandIndigo else TextSecondary,
                contentColor = if (registration.attending == AttendingStatus.YES) BrandIndigo else TextSecondary
            )
        }
        if (!registration.email.isNullOrBlank()) {
            Text(registration.email, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!registration.phone.isNullOrBlank()) {
            Text(registration.phone, color = TextSecondary, fontSize = 12.sp)
        }
        if (registration.guestCount > 0) {
            Text("+${registration.guestCount} guest${if (registration.guestCount == 1) "" else "s"}", color = TextSecondary, fontSize = 12.sp)
        }
        if (!registration.notes.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(registration.notes, color = TextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text("Registered ${formatShortDate(registration.createdAt)}", color = TextSecondary, fontSize = 11.sp)
    }
}

/** PDF table is narrower than the CSV — Notes is dropped there to leave room for the other columns. */
private fun registrationRow(registration: RsvpRegistration): List<String> = listOf(
    registration.name,
    registration.email.orEmpty(),
    registration.phone.orEmpty(),
    registration.attending.label,
    registration.guestCount.toString(),
    formatDateTime(registration.createdAt)
)

private fun registrationCsvRow(registration: RsvpRegistration): List<String> = listOf(
    registration.name,
    registration.email.orEmpty(),
    registration.phone.orEmpty(),
    registration.attending.label,
    registration.guestCount.toString(),
    registration.notes.orEmpty(),
    formatDateTime(registration.createdAt)
)
