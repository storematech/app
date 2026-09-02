package com.quizmaker.android.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.data.model.FEEDBACK_FORM_TEMPLATES
import com.quizmaker.android.data.model.FeedbackFormTemplate
import com.quizmaker.android.data.model.ONBOARDING_FORM_TEMPLATES
import com.quizmaker.android.data.model.OnboardingFormTemplate
import com.quizmaker.android.data.model.POLL_TEMPLATES
import com.quizmaker.android.data.model.PollTemplate
import com.quizmaker.android.data.model.RSVP_EVENT_TEMPLATES
import com.quizmaker.android.data.model.RsvpEventTemplate
import com.quizmaker.android.data.model.VOTING_TEMPLATES
import com.quizmaker.android.data.model.VotingTemplate
import com.quizmaker.android.ui.common.elevatedSurface

/** [onSelect*Template] land straight in that tool's create sheet, same as ExploreTemplatesScreen —
 *  see NavGraph.kt's navigateWithTemplate. More → Tools — the five web "Tools" migrated to mobile,
 *  each's own row followed (after every row) by all five template sections so someone can start
 *  from a template without leaving this hub at all. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateBack: () -> Unit,
    onOpenOnboardingForms: () -> Unit,
    onOpenFeedbackForms: () -> Unit,
    onOpenPolls: () -> Unit,
    onOpenVoting: () -> Unit,
    onOpenRsvpEvents: () -> Unit,
    onSelectOnboardingTemplate: (OnboardingFormTemplate) -> Unit,
    onSelectFeedbackTemplate: (FeedbackFormTemplate) -> Unit,
    onSelectPollTemplate: (PollTemplate) -> Unit,
    onSelectVotingTemplate: (VotingTemplate) -> Unit,
    onSelectRsvpTemplate: (RsvpEventTemplate) -> Unit
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            // The LazyColumn below already insets everything (rows and sections alike) by 20dp
            // horizontally via its own contentPadding, so each TemplateSection's cardWidth is
            // computed off that already-reduced width, and its own horizontalPadding is 0 — passing
            // 20dp again here would double the inset.
            val cardWidth = templateRowCardWidth(maxWidth - 40.dp, horizontalPadding = 0.dp)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ToolMenuRow(
                        icon = Icons.Default.Assignment,
                        title = "Onboarding Form",
                        subtitle = "Collect new-learner details with a shareable form",
                        onClick = onOpenOnboardingForms
                    )
                }
                item {
                    ToolMenuRow(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = "Feedback Form",
                        subtitle = "Gather feedback after a session or course",
                        onClick = onOpenFeedbackForms
                    )
                }
                item {
                    ToolMenuRow(
                        icon = Icons.Default.BarChart,
                        title = "Poll",
                        subtitle = "Quick single-question polls",
                        onClick = onOpenPolls
                    )
                }
                item {
                    ToolMenuRow(
                        icon = Icons.Default.CheckCircle,
                        title = "Voting",
                        subtitle = "Vote on options, like where to go for lunch",
                        onClick = onOpenVoting
                    )
                }
                item {
                    ToolMenuRow(
                        icon = Icons.Default.CalendarToday,
                        title = "RSVP and Events",
                        subtitle = "Collect RSVPs for an event",
                        onClick = onOpenRsvpEvents
                    )
                }
                item { Spacer(Modifier.height(14.dp)) }
                item {
                    TemplateSection(
                        "Onboarding Forms", ONBOARDING_FORM_TEMPLATES, { it.icon }, { it.label }, { it.summary },
                        cardWidth, horizontalPadding = 0.dp, onSelect = onSelectOnboardingTemplate
                    )
                }
                item { Spacer(Modifier.height(30.dp)) }
                item {
                    TemplateSection(
                        "Feedback Forms", FEEDBACK_FORM_TEMPLATES, { it.icon }, { it.label }, { it.summary },
                        cardWidth, horizontalPadding = 0.dp, onSelect = onSelectFeedbackTemplate
                    )
                }
                item { Spacer(Modifier.height(30.dp)) }
                item {
                    TemplateSection(
                        "Polls", POLL_TEMPLATES, { it.icon }, { it.label }, { it.summary },
                        cardWidth, horizontalPadding = 0.dp, onSelect = onSelectPollTemplate
                    )
                }
                item { Spacer(Modifier.height(30.dp)) }
                item {
                    TemplateSection(
                        "Voting", VOTING_TEMPLATES, { it.icon }, { it.label }, { it.summary },
                        cardWidth, horizontalPadding = 0.dp, onSelect = onSelectVotingTemplate
                    )
                }
                item { Spacer(Modifier.height(30.dp)) }
                item {
                    TemplateSection(
                        "RSVP and Events", RSVP_EVENT_TEMPLATES, { it.icon }, { it.label }, { it.summary },
                        cardWidth, horizontalPadding = 0.dp, onSelect = onSelectRsvpTemplate
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolMenuRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
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
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = TextSecondary, fontSize = 12.5.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}
