package com.quizmaker.android.ui.tools

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChecklistRtl
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.elevatedSurface

/** More → Tools. Onboarding Form is the first of six web "Tools" migrated to mobile; the rest route to the existing Coming Soon screen until built. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateBack: () -> Unit,
    onOpenOnboardingForms: () -> Unit,
    onOpenFeedbackForms: () -> Unit,
    onOpenPolls: () -> Unit,
    onOpenVoting: () -> Unit,
    onOpenRsvpEvents: () -> Unit,
    onOpenComingSoon: (String) -> Unit
) {
    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text("Tools", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ToolMenuRow(
                    icon = Icons.Default.Assignment,
                    title = "Onboarding Form",
                    subtitle = "Collect new-learner details with a shareable form",
                    comingSoon = false,
                    onClick = onOpenOnboardingForms
                )
            }
            item {
                ToolMenuRow(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = "Feedback Form",
                    subtitle = "Gather feedback after a session or course",
                    comingSoon = false,
                    onClick = onOpenFeedbackForms
                )
            }
            item {
                ToolMenuRow(
                    icon = Icons.Default.BarChart,
                    title = "Poll",
                    subtitle = "Quick single-question polls",
                    comingSoon = false,
                    onClick = onOpenPolls
                )
            }
            item {
                ToolMenuRow(
                    icon = Icons.Default.CheckCircle,
                    title = "Voting",
                    subtitle = "Vote on options, like where to go for lunch",
                    comingSoon = false,
                    onClick = onOpenVoting
                )
            }
            item {
                ToolMenuRow(
                    icon = Icons.Default.CalendarToday,
                    title = "RSVP and Events",
                    subtitle = "Collect RSVPs for an event",
                    comingSoon = false,
                    onClick = onOpenRsvpEvents
                )
            }
            item {
                ToolMenuRow(
                    icon = Icons.Default.ChecklistRtl,
                    title = "Survey",
                    subtitle = "Multi-question surveys",
                    comingSoon = true,
                    onClick = { onOpenComingSoon("Survey") }
                )
            }
        }
    }
}

@Composable
private fun ToolMenuRow(icon: ImageVector, title: String, subtitle: String, comingSoon: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(BrandIndigoLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                if (comingSoon) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, BorderGray, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Coming soon", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = TextSecondary, fontSize = 12.5.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}
