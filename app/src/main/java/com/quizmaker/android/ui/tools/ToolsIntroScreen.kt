package com.quizmaker.android.ui.tools

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.StatAmberBg
import com.quizmaker.android.core.theme.StatAmberIcon
import com.quizmaker.android.core.theme.StatBlueBg
import com.quizmaker.android.core.theme.StatBlueIcon
import com.quizmaker.android.core.theme.StatGreenBg
import com.quizmaker.android.core.theme.StatGreenIcon
import com.quizmaker.android.core.theme.StatPurpleBg
import com.quizmaker.android.core.theme.StatPurpleIcon
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.elevatedSurface
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One-time "what Tools can do" interstitial shown the first time an account opens More → Tools —
 * see ToolsIntroViewModel.shouldShowIntro() for the once-per-account+device gating, mirroring
 * NotificationPermissionScreen's pattern.
 */
@Composable
fun ToolsIntroScreen(
    onUseTools: () -> Unit,
    viewModel: ToolsIntroViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .elevatedSurface(shape = RoundedCornerShape(20.dp))
                .padding(vertical = 16.dp)
        ) {
            ToolsInboxAnimation()
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "Everything lands in one Inbox",
            textAlign = TextAlign.Center,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            color = TextPrimary
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Onboarding forms, feedback, polls and voting — every response from every Tool shows up right here, so you never have to go looking for it.",
            textAlign = TextAlign.Center,
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(32.dp))
        GradientButton(
            text = "Use Tools",
            onClick = {
                viewModel.markIntroShown(used = true)
                onUseTools()
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "Maybe later",
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.clickable {
                viewModel.markIntroShown(used = false)
                onUseTools()
            }
        )
        Spacer(Modifier.height(24.dp))
    }
}

private data class ToolSource(val icon: ImageVector, val bg: Color, val tint: Color)

/**
 * Small looping scene: one submission from each Tool travels down in turn and lands in a shared
 * Inbox bubble, which bounces and ticks its unread badge up to 4 before a "New responses!" toast
 * confirms — showing what the Inbox actually collects rather than just describing it in a sentence.
 */
@Composable
private fun ToolsInboxAnimation(modifier: Modifier = Modifier) {
    // Reads StatBlueBg/StatGreenBg/StatAmberBg/StatPurpleBg, which are @Composable (theme-aware —
    // see Color.kt), so this can't be a top-level val — a plain local val instead (not remember{},
    // so it still picks up a live theme change rather than caching whichever color was current the
    // first time this composed).
    val toolSources = listOf(
        ToolSource(Icons.Default.Assignment, StatBlueBg, StatBlueIcon), // Onboarding
        ToolSource(Icons.Default.ChatBubbleOutline, StatGreenBg, StatGreenIcon), // Feedback
        ToolSource(Icons.Default.BarChart, StatAmberBg, StatAmberIcon), // Poll
        ToolSource(Icons.Default.CheckCircle, StatPurpleBg, StatPurpleIcon) // Voting
    )
    val chipProgress = remember { Animatable(0f) }
    val chipScale = remember { Animatable(0f) }
    val inboxScale = remember { Animatable(1f) }
    val toastAlpha = remember { Animatable(0f) }
    var activeIndex by remember { mutableIntStateOf(0) }
    var badgeCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            for (index in toolSources.indices) {
                activeIndex = index
                chipProgress.snapTo(0f)
                chipScale.snapTo(0f)

                // Submission appears at its Tool's bubble, then travels down into the Inbox.
                chipScale.animateTo(1f, tween(180, easing = FastOutSlowInEasing))
                delay(80)
                chipProgress.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
                chipScale.animateTo(0f, tween(140))

                // Arrival: Inbox bounces and its unread badge ticks up by one.
                coroutineScope {
                    launch {
                        inboxScale.animateTo(1.22f, tween(130, easing = FastOutSlowInEasing))
                        inboxScale.animateTo(1f, tween(170, easing = FastOutSlowInEasing))
                    }
                }
                badgeCount = index + 1
                delay(160)
            }

            toastAlpha.animateTo(1f, tween(200))
            delay(1200)
            toastAlpha.animateTo(0f, tween(250))
            delay(450)
            badgeCount = 0
            delay(300)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .padding(horizontal = 24.dp)
    ) {
        val sourceBubbleSize = 46.dp
        val inboxSize = 64.dp
        val chipSize = sourceBubbleSize * 0.55f
        val lineColor = BorderGray

        Canvas(modifier = Modifier.fillMaxSize()) {
            val endPoint = Offset(size.width / 2f, size.height - inboxSize.toPx() / 2f)
            toolSources.indices.forEach { index ->
                val startPoint = Offset(size.width * ((index + 0.5f) / toolSources.size), sourceBubbleSize.toPx() / 2f)
                drawLine(
                    color = lineColor,
                    start = startPoint,
                    end = endPoint,
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f))
                )
            }
        }

        toolSources.forEachIndexed { index, source ->
            val centerXFraction = (index + 0.5f) / toolSources.size
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = maxWidth * centerXFraction - sourceBubbleSize / 2, y = 0.dp)
                    .size(sourceBubbleSize)
                    .clip(CircleShape)
                    .background(source.bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(source.icon, contentDescription = null, tint = source.tint, modifier = Modifier.size(20.dp))
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(inboxSize)
                    .scale(inboxScale.value)
                    .clip(CircleShape)
                    .background(BrandIndigoLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Inbox, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(28.dp))
            }
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(ErrorRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(badgeCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (chipScale.value > 0f) {
            val source = toolSources[activeIndex]
            val startXFraction = (activeIndex + 0.5f) / toolSources.size
            val startX = maxWidth * startXFraction - chipSize / 2
            val endX = maxWidth / 2 - chipSize / 2
            val endY = maxHeight - inboxSize / 2 - chipSize / 2
            Box(
                modifier = Modifier
                    .offset(
                        x = lerp(startX, endX, chipProgress.value),
                        y = lerp(0.dp, endY, chipProgress.value)
                    )
                    .size(chipSize)
                    .scale(chipScale.value)
                    .clip(CircleShape)
                    .background(source.tint),
                contentAlignment = Alignment.Center
            ) {
                Icon(source.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }

        if (toastAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .alpha(toastAlpha.value)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SuccessGreen)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("New responses!", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
