package com.quizmaker.android.ui.featuretour

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.PremiumGoldEnd
import com.quizmaker.android.core.theme.PremiumGoldStart
import com.quizmaker.android.core.theme.StatAmberBg
import com.quizmaker.android.core.theme.StatAmberIcon
import com.quizmaker.android.core.theme.StatGreenBg
import com.quizmaker.android.core.theme.StatGreenIcon
import com.quizmaker.android.core.theme.StatPurpleBg
import com.quizmaker.android.core.theme.StatPurpleIcon
import com.quizmaker.android.core.theme.StatRedBg
import com.quizmaker.android.core.theme.StatRedIcon
import com.quizmaker.android.core.theme.StatTealBg
import com.quizmaker.android.core.theme.StatTealIcon
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.TrialPaywallSheet
import com.quizmaker.android.ui.common.elevatedSurface
import com.quizmaker.android.ui.pricing.SUPPORT_WHATSAPP_URL

/**
 * "View Feature" playground — an animated, no-login walkthrough of what the app can do, reached
 * from [com.quizmaker.android.ui.common.FeatureTourBanner] on the Dashboard (shown only to
 * non-premium accounts — see DashboardScreen's gating). Every section below is a static
 * description plus a small looping demo (see FeatureTourDemos.kt). Only 7 of the 9 feature cards
 * get a "jump straight there" CTA — Submitting the Quiz and Reports don't, since neither has one
 * specific destination screen the way the rest do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureTourScreen(
    onNavigateBack: () -> Unit,
    onOpenPricing: () -> Unit,
    onOpenAi: () -> Unit,
    onCreateQuiz: () -> Unit,
    onOpenRevision: () -> Unit,
    onOpenClasses: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenLearners: () -> Unit,
    onOpenReportedQuestions: () -> Unit,
    viewModel: FeatureTourViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var isPaused by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
        topBar = {
            TopAppBar(
                title = { Text("Explore Features", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isPaused = !isPaused }) {
                        Icon(
                            if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume animations" else "Pause animations"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text("See it in action", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "A quick animated tour of everything Yuno LMS can do for you",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            item {
                FeatureTourCard(
                    number = 1,
                    title = "Create Quiz with AI",
                    tagline = "Type a topic, get a full quiz in seconds",
                    icon = Icons.Default.AutoAwesome,
                    accent = BrandIndigo,
                    accentBg = BrandIndigoLight,
                    steps = listOf(
                        "Tap AI on your dashboard",
                        "Type a topic or paste your content",
                        "Tap Send — your quiz is generated in seconds"
                    ),
                    demo = { AiQuizCreateDemo(paused = isPaused) },
                    ctaLabel = "Create Quiz with AI",
                    onCtaClick = { viewModel.onOpenAiClick(onOpenAi) }
                )
            }
            item { ReceiptTearDivider() }

            item {
                FeatureTourCard(
                    number = 2,
                    title = "Create Quiz",
                    tagline = "Build one by hand, exactly the way you want it",
                    icon = Icons.Default.NoteAdd,
                    accent = BrandIndigo,
                    accentBg = BrandIndigoLight,
                    steps = listOf(
                        "Tap Create Quiz and give it a title",
                        "Turn on Leaderboard to add friendly competition",
                        "Set a timer for the whole quiz or per question",
                        "Share the link or QR with your learners"
                    ),
                    demo = { ManualQuizCreateDemo(paused = isPaused) },
                    ctaLabel = "Create Quiz",
                    onCtaClick = { viewModel.onCreateQuizClick(onCreateQuiz) }
                )
            }
            item { ReceiptTearDivider() }

            item {
                FeatureTourCard(
                    number = 3,
                    title = "Submitting the Quiz",
                    tagline = "No login for learners — and you're notified instantly",
                    icon = Icons.Default.Assessment,
                    accent = StatGreenIcon,
                    accentBg = StatGreenBg,
                    steps = listOf(
                        "Learners open the quiz link — no login needed",
                        "They answer and tap Submit",
                        "You get notified the moment a submission comes in"
                    ),
                    demo = { QuizSubmissionDemo(paused = isPaused) }
                )
            }
            item { ReceiptTearDivider() }

            item {
                FeatureTourCard(
                    number = 4,
                    title = "Reports",
                    tagline = "Live scores, analysis, and exports",
                    icon = Icons.Default.Assessment,
                    accent = StatPurpleIcon,
                    accentBg = StatPurpleBg,
                    steps = listOf(
                        "Open any quiz to see live scores and analysis",
                        "Export the answer key or a blank paper as PDF",
                        "Download a leaderboard or full report anytime"
                    ),
                    demo = { ReportsDemo(paused = isPaused) }
                )
            }
            item { ReceiptTearDivider() }

            item {
                FeatureTourCard(
                    number = 5,
                    title = "Revision",
                    tagline = "Turns weak topics into a retest automatically",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    accent = StatPurpleIcon,
                    accentBg = StatPurpleBg,
                    steps = listOf(
                        "Yuno LMS finds each learner's weak topics automatically",
                        "Create a retest with one tap",
                        "Track improvement over time"
                    ),
                    demo = {
                        FeatureSpotlightDemo(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            accent = StatPurpleIcon,
                            accentBg = StatPurpleBg,
                            bullets = listOf("Auto-picks weak topics", "Retest in one tap", "Tracks improvement"),
                            paused = isPaused
                        )
                    },
                    ctaLabel = "Revision List",
                    onCtaClick = onOpenRevision
                )
            }
            item { ReceiptTearDivider() }

            item {
                FeatureTourCard(
                    number = 6,
                    title = "Classes",
                    tagline = "Group learners and see how each class is doing",
                    icon = Icons.Default.School,
                    accent = StatTealIcon,
                    accentBg = StatTealBg,
                    steps = listOf(
                        "Group learners into classes",
                        "See a class-wise leaderboard",
                        "Spot learners who need extra help"
                    ),
                    demo = {
                        FeatureSpotlightDemo(
                            icon = Icons.Default.School,
                            accent = StatTealIcon,
                            accentBg = StatTealBg,
                            bullets = listOf("Class-wise leaderboard", "Weak-learner alerts", "Organized rosters"),
                            paused = isPaused
                        )
                    },
                    ctaLabel = "Create Class",
                    onCtaClick = onOpenClasses
                )
            }
            item { ReceiptTearDivider() }

            item {
                FeatureTourCard(
                    number = 7,
                    title = "Tools",
                    tagline = "Forms, polls, voting, and RSVPs — beyond quizzes",
                    icon = Icons.Default.Construction,
                    accent = StatAmberIcon,
                    accentBg = StatAmberBg,
                    steps = listOf(
                        "Collect onboarding & feedback forms",
                        "Run quick polls and voting",
                        "Manage RSVPs for events"
                    ),
                    demo = {
                        FeatureSpotlightDemo(
                            icon = Icons.Default.Construction,
                            accent = StatAmberIcon,
                            accentBg = StatAmberBg,
                            bullets = listOf("Onboarding & feedback forms", "Polls & voting", "Event RSVPs"),
                            paused = isPaused
                        )
                    },
                    ctaLabel = "Explore Tools",
                    onCtaClick = onOpenTools
                )
            }
            item { ReceiptTearDivider() }

            item {
                FeatureTourCard(
                    number = 8,
                    title = "Learners",
                    tagline = "A living roster, built automatically",
                    icon = Icons.Default.Group,
                    accent = StatGreenIcon,
                    accentBg = StatGreenBg,
                    steps = listOf(
                        "Every learner who takes a quiz is added automatically",
                        "View full quiz history per learner",
                        "Organize and search your roster anytime"
                    ),
                    demo = {
                        FeatureSpotlightDemo(
                            icon = Icons.Default.Group,
                            accent = StatGreenIcon,
                            accentBg = StatGreenBg,
                            bullets = listOf("Auto-added from submissions", "Full quiz history", "Searchable roster"),
                            paused = isPaused
                        )
                    },
                    ctaLabel = "Add Learners",
                    onCtaClick = onOpenLearners
                )
            }
            item { ReceiptTearDivider() }

            item {
                FeatureTourCard(
                    number = 9,
                    title = "Reported Questions",
                    tagline = "Learners flag issues, you fix them in one place",
                    icon = Icons.Default.Flag,
                    accent = StatRedIcon,
                    accentBg = StatRedBg,
                    steps = listOf(
                        "Learners can flag a confusing question",
                        "Review flagged questions in one place",
                        "Fix it and keep your question bank clean"
                    ),
                    demo = {
                        FeatureSpotlightDemo(
                            icon = Icons.Default.Flag,
                            accent = StatRedIcon,
                            accentBg = StatRedBg,
                            bullets = listOf("Learner-flagged questions", "Review in one place", "Cleaner question bank"),
                            paused = isPaused
                        )
                    },
                    ctaLabel = "Check Reported Questions",
                    onCtaClick = onOpenReportedQuestions
                )
            }

            item { ViewPlanCard(onClick = onOpenPricing) }

            item {
                BookDemoCard(
                    onClick = {
                        val message = Uri.encode("Hi, I'd like to book a demo of Yuno LMS")
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$SUPPORT_WHATSAPP_URL?text=$message")))
                    }
                )
            }
        }
    }

    if (uiState.showTrialPaywall) {
        TrialPaywallSheet(onDismiss = viewModel::dismissTrialPaywall, onViewPlans = onOpenPricing)
    }
}

@Composable
private fun FeatureTourCard(
    number: Int,
    title: String,
    tagline: String,
    icon: ImageVector,
    accent: Color,
    accentBg: Color,
    steps: List<String>,
    demo: @Composable () -> Unit,
    ctaLabel: String? = null,
    onCtaClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).background(accentBg),
                contentAlignment = Alignment.Center
            ) {
                Text("$number", color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                Text(tagline, color = TextSecondary, fontSize = 11.5.sp)
            }
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(AppBackground)
        ) {
            demo()
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier.size(16.dp).clip(CircleShape).background(accentBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", color = accent, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(step, color = TextSecondary, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                }
            }
        }
        // Light-tint background + saturated text (the same accent/accentBg pairing as the badges
        // above), not a solid/bright fill — a loud CTA here would fight the step list right above
        // it for attention and the white-on-bright-color combination reads poorly against every
        // one of these nine accent colors, not just a couple.
        if (ctaLabel != null && onCtaClick != null) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(accentBg)
                    .clickable(onClick = onCtaClick)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(ctaLabel, color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/**
 * Divider between two feature cards, styled like a thermal-printer receipt's tear line — a dashed
 * rule broken by a small scissors badge, with two notch circles (painted the screen's own
 * background color, so they read as cut-outs) at the ends. Only used between the numbered feature
 * cards (see FeatureTourScreen's item list) — the plain [Arrangement.spacedBy] gap everywhere else
 * (header→card 1, card 9→the plan/demo CTAs) stays a plain gap since those aren't "one feature to
 * the next."
 */
@Composable
private fun ReceiptTearDivider() {
    val lineColor = BorderGray
    val notchColor = AppBackground
    val badgeColor = SurfaceWhite
    Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
            val y = size.height / 2f
            val notchRadius = 6.dp.toPx()
            val badgeHalfWidth = 17.dp.toPx()
            val strokeWidth = 2.dp.toPx()
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            drawLine(
                color = lineColor,
                start = Offset(notchRadius * 2, y),
                end = Offset(size.width / 2f - badgeHalfWidth, y),
                strokeWidth = strokeWidth,
                pathEffect = dashEffect
            )
            drawLine(
                color = lineColor,
                start = Offset(size.width / 2f + badgeHalfWidth, y),
                end = Offset(size.width - notchRadius * 2, y),
                strokeWidth = strokeWidth,
                pathEffect = dashEffect
            )
            drawCircle(color = notchColor, radius = notchRadius, center = Offset(notchRadius, y))
            drawCircle(color = notchColor, radius = notchRadius, center = Offset(size.width - notchRadius, y))
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(badgeColor)
                .border(1.dp, lineColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ContentCut, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun ViewPlanCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(PremiumGoldStart, PremiumGoldEnd)))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Unlock Everything", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(
                "Advanced reports, negative marking, business branding & more",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("View Plans", color = PremiumGoldStart, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PremiumGoldStart, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun BookDemoCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .elevatedSurface(shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(BrandIndigoLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Want a Live Walkthrough?", fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text("Book a free demo with our team on WhatsApp", color = TextSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
    }
}
