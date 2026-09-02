package com.quizmaker.android.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SurfaceWhite
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
import com.quizmaker.android.ui.common.TEMPLATE_GRADIENTS
import com.quizmaker.android.ui.common.TemplateGradient

/**
 * Single "browse every tool's templates" screen — one section per tool (Onboarding, Feedback,
 * Poll, Voting, RSVP), each its own title followed by a horizontally-swipeable row of that tool's
 * 10 templates, stacked top to bottom with breathing room between sections. Replaces the earlier
 * design of 5 separate per-tool gallery screens: this way a "Templates" tap from any tool's list
 * lands on the same explorer, and picking a template from *any* section — not just the one you
 * came from — is equally one tap away.
 *
 * Each `onSelect*` callback is wired in NavGraph.kt to jump straight to that template's own tool
 * screen with its create sheet already open (see ExploreTemplates handling there) — including when
 * the picked template belongs to a different tool than the one the user started from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreTemplatesScreen(
    onNavigateBack: () -> Unit,
    onSelectOnboarding: (OnboardingFormTemplate) -> Unit,
    onSelectFeedback: (FeedbackFormTemplate) -> Unit,
    onSelectPoll: (PollTemplate) -> Unit,
    onSelectVoting: (VotingTemplate) -> Unit,
    onSelectRsvp: (RsvpEventTemplate) -> Unit
) {
    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text("Explore Templates", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Reliable "2.5 cards" scroll hint (see templateRowCardWidth's own doc) instead of
            // leaving it to whatever happens to be left over from a fixed card width, and starting
            // the first section a fifth of the way down the screen instead of right under the top
            // bar — both requested so it reads immediately as "browse", not "here's the whole list".
            val cardWidth = templateRowCardWidth(maxWidth, horizontalPadding = 20.dp)
            val topSpacerHeight = maxHeight * 0.2f
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(topSpacerHeight))
                TemplateSection("Onboarding Forms", ONBOARDING_FORM_TEMPLATES, { it.icon }, { it.label }, { it.summary }, cardWidth, onSelect = onSelectOnboarding)
                Spacer(Modifier.height(36.dp))
                TemplateSection("Feedback Forms", FEEDBACK_FORM_TEMPLATES, { it.icon }, { it.label }, { it.summary }, cardWidth, onSelect = onSelectFeedback)
                Spacer(Modifier.height(36.dp))
                TemplateSection("Polls", POLL_TEMPLATES, { it.icon }, { it.label }, { it.summary }, cardWidth, onSelect = onSelectPoll)
                Spacer(Modifier.height(36.dp))
                TemplateSection("Voting", VOTING_TEMPLATES, { it.icon }, { it.label }, { it.summary }, cardWidth, onSelect = onSelectVoting)
                Spacer(Modifier.height(36.dp))
                TemplateSection("RSVP and Events", RSVP_EVENT_TEMPLATES, { it.icon }, { it.label }, { it.summary }, cardWidth, onSelect = onSelectRsvp)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Card width that reliably shows ~2 full cards plus a third one peeking off the trailing edge —
 *  the "there's more, scroll" hint — instead of a fixed width that on some screens happens to end
 *  exactly at the viewport edge with barely any peek. [availableWidth] is the container's full width
 *  (a plain screen-width BoxWithConstraints reading, or minus a parent's own horizontal contentPadding
 *  when this section sits inside one — see ToolsScreen's usage); [horizontalPadding] must match
 *  whatever's passed to [TemplateSection] as its own row inset. */
internal fun templateRowCardWidth(availableWidth: Dp, horizontalPadding: Dp): Dp =
    ((availableWidth - horizontalPadding - 24.dp) / 2.5f).coerceIn(120.dp, 190.dp)

/** Reused by both ExploreTemplatesScreen (one section per tool, full-page) and ToolsScreen (the
 *  same sections embedded under its 5 tool rows) — see templateRowCardWidth for [cardWidth] and
 *  [horizontalPadding]'s relationship (the latter must match whatever the caller's own outer
 *  container already insets by, e.g. 0.dp when a parent LazyColumn's contentPadding already does it). */
@Composable
internal fun <T> TemplateSection(
    title: String,
    templates: List<T>,
    icon: (T) -> ImageVector,
    label: (T) -> String,
    summary: (T) -> String,
    cardWidth: Dp,
    horizontalPadding: Dp = 20.dp,
    onSelect: (T) -> Unit
) {
    Column {
        Text(
            title,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = horizontalPadding)
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = horizontalPadding)
        ) {
            items(templates.size) { index ->
                val template = templates[index]
                TemplateRowCard(
                    width = cardWidth,
                    icon = icon(template),
                    label = label(template),
                    summary = summary(template),
                    gradient = TEMPLATE_GRADIENTS[index % TEMPLATE_GRADIENTS.size],
                    onClick = { onSelect(template) }
                )
            }
        }
    }
}

@Composable
private fun TemplateRowCard(width: Dp, icon: ImageVector, label: String, summary: String, gradient: TemplateGradient, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(width)
            .height(140.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(gradient.start, gradient.end)))
            .clickable(onClick = onClick)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.16f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 18.dp, y = 18.dp)
                .size(76.dp)
                .rotate(-15f)
        )
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                label,
                color = Color.White,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                summary,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Pill CTA that opens [ExploreTemplatesScreen] from any tool's list screen — see [ToolsFabOverlay]
 *  for how it's positioned (right side, off the very bottom edge, not Scaffold's default FAB spot). */
@Composable
fun TemplatesPillButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(50), ambientColor = BrandIndigo.copy(alpha = 0.25f), spotColor = BrandIndigo.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(50))
            .background(SurfaceWhite)
            .border(1.dp, BrandIndigo, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Dashboard, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Templates", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/**
 * Wraps a Tool list screen's own `Scaffold(...)` call and overlays [TemplatesPillButton] pinned to
 * the right edge, roughly a fifth of the way up from the bottom — deliberately not Scaffold's
 * built-in `floatingActionButton` slot, which only offers flush-bottom-center/-end positions, not
 * this "off the bottom edge" placement.
 */
@Composable
fun ToolsFabOverlay(onOpenTemplates: () -> Unit, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        content()
        TemplatesPillButton(
            onClick = onOpenTemplates,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = maxHeight * 0.2f)
        )
    }
}
