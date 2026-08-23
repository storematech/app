package com.quizmaker.android.ui.featuretour

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * Every demo in this file is a silent, looping storyboard — `LaunchedEffect(Unit) { while (true) {...} }`
 * driving plain `mutableStateOf`/`Animatable` fields through a fixed sequence of `delay()`s, no
 * touch input involved. Pure Compose (same reasoning as SuccessCheckmark.kt: no Lottie/GIF
 * dependency needed for animations this simple), designed to sit inside a fixed-height preview box
 * on FeatureTourScreen's cards and just keep replaying for as long as that card is on screen.
 */

@Composable
fun AiQuizCreateDemo(paused: Boolean) {
    val prompt = "Photosynthesis for grade 8"
    var typedChars by remember { mutableStateOf(0) }
    var phase by remember { mutableStateOf(0) } // 0 = typing, 1 = generating, 2 = result

    LaunchedEffectLoop(paused) {
        typedChars = 0
        phase = 0
        while (typedChars < prompt.length) {
            delay(55)
            typedChars++
        }
        delay(400)
        phase = 1
        delay(1400)
        phase = 2
        delay(1900)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
        Text("Describe your quiz", color = TextSecondary, fontSize = 10.sp)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceWhite)
                .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                prompt.take(typedChars),
                color = TextPrimary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).background(if (phase >= 1) BrandIndigo else BrandIndigoLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    tint = if (phase >= 1) Color.White else BrandIndigo,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        AnimatedVisibility(visible = phase == 1, enter = fadeIn(), exit = fadeOut()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = BrandIndigo)
                Spacer(Modifier.width(8.dp))
                Text("Generating quiz…", color = TextSecondary, fontSize = 12.sp)
            }
        }
        AnimatedVisibility(visible = phase == 2, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SuccessGreen.copy(alpha = 0.12f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Quiz generated — 10 questions ready", color = SuccessGreen, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
            }
        }
    }
}

@Composable
fun ManualQuizCreateDemo(paused: Boolean) {
    val title = "Weekly Vocabulary Quiz"
    var typedChars by remember { mutableStateOf(0) }
    var leaderboardOn by remember { mutableStateOf(false) }
    var timerMinutes by remember { mutableStateOf(0) }
    var sent by remember { mutableStateOf(false) }

    LaunchedEffectLoop(paused) {
        typedChars = 0
        leaderboardOn = false
        timerMinutes = 0
        sent = false
        while (typedChars < title.length) {
            delay(45)
            typedChars++
        }
        delay(450)
        leaderboardOn = true
        delay(650)
        while (timerMinutes < 10) {
            delay(90)
            timerMinutes += 2
        }
        delay(550)
        sent = true
        delay(1900)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column {
            Text("Quiz Title", color = TextSecondary, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(title.take(typedChars), color = TextPrimary, fontSize = 12.sp, maxLines = 1)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Leaderboard", color = TextPrimary, fontSize = 12.sp)
            }
            MiniToggle(on = leaderboardOn)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Timer", color = TextPrimary, fontSize = 12.sp)
            }
            Text("$timerMinutes min", color = BrandIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(if (sent) SuccessGreen else BrandIndigo)
                .padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (sent) Icons.Default.CheckCircle else Icons.Default.Send,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(if (sent) "Sent to learners" else "Send to Learners", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun QuizSubmissionDemo(paused: Boolean) {
    val options = listOf("Mitosis", "Photosynthesis", "Respiration", "Digestion")
    var selectedOption by remember { mutableStateOf(-1) }
    var submitted by remember { mutableStateOf(false) }
    var showNotification by remember { mutableStateOf(false) }

    LaunchedEffectLoop(paused) {
        selectedOption = -1
        submitted = false
        showNotification = false
        delay(600)
        selectedOption = 1
        delay(700)
        submitted = true
        delay(500)
        showNotification = true
        delay(1900)
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            Text("Q1. Which process makes food in plants?", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
            Spacer(Modifier.height(10.dp))
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedOption
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) BrandIndigoLight else SurfaceWhite)
                        .border(1.dp, if (isSelected) BrandIndigo else BorderGray, RoundedCornerShape(9.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(14.dp).clip(CircleShape).border(1.5.dp, if (isSelected) BrandIndigo else BorderGray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(BrandIndigo))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(option, color = TextPrimary, fontSize = 11.sp)
                }
                if (index != options.lastIndex) Spacer(Modifier.height(5.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(if (submitted) SuccessGreen else BrandIndigo)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(if (submitted) "Submitted ✓" else "Submit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        AnimatedVisibility(
            visible = showNotification,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(26.dp).clip(CircleShape).background(BrandIndigoLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("New submission", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text("Priya Sharma completed the quiz", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun ReportsDemo(paused: Boolean) {
    val bars = listOf(0.9f, 0.55f, 0.75f, 0.4f)
    var revealedCount by remember { mutableStateOf(0) }
    var showBadge by remember { mutableStateOf(false) }

    LaunchedEffectLoop(paused) {
        revealedCount = 0
        showBadge = false
        for (i in bars.indices) {
            delay(280)
            revealedCount = i + 1
        }
        delay(400)
        showBadge = true
        delay(1900)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Class Performance", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            AnimatedVisibility(visible = showBadge, enter = fadeIn(), exit = fadeOut()) {
                Box(modifier = Modifier.clip(RoundedCornerShape(50)).background(SuccessGreen.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                    Text("82% avg", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(70.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEachIndexed { index, target ->
                val fraction by animateFloatAsState(
                    targetValue = if (index < revealedCount) target else 0f,
                    animationSpec = tween(450),
                    label = "bar"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(BrandIndigo)
                )
            }
        }
    }
}

@Composable
fun FeatureSpotlightDemo(icon: ImageVector, accent: Color, accentBg: Color, bullets: List<String>, paused: Boolean) {
    var visibleBullets by remember { mutableStateOf(0) }
    val iconScale = remember { Animatable(0.7f) }

    LaunchedEffectLoop(paused) {
        visibleBullets = 0
        iconScale.snapTo(0.7f)
        iconScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        for (i in bullets.indices) {
            delay(480)
            visibleBullets = i + 1
        }
        delay(1700)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(46.dp).scale(iconScale.value).clip(CircleShape).background(accentBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(10.dp))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            bullets.forEachIndexed { index, bullet ->
                AnimatedVisibility(
                    visible = index < visibleBullets,
                    enter = fadeIn() + slideInHorizontally(initialOffsetX = { -it / 3 }),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(accentBg).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(bullet, color = TextPrimary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/** Small on/off pill, hand-drawn rather than Material3's [androidx.compose.material3.Switch] —
 *  these demos are display-only mockups, not real controls, and a tiny custom pill scales down to
 *  fit the compact card without fighting Switch's built-in minimum touch-target sizing. */
@Composable
private fun MiniToggle(on: Boolean) {
    val offset by animateFloatAsState(targetValue = if (on) 16f else 0f, animationSpec = tween(300), label = "toggle")
    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 18.dp)
            .clip(RoundedCornerShape(50))
            .background(if (on) SuccessGreen else BorderGray)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset.dp)
                .size(14.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * `LaunchedEffect(Unit) { while (true) { block(); } }` — every demo above is this same shape, just
 * with a different sequence of state changes and delays inside [block]. Keying the effect on
 * [paused] means flipping it to true cancels the in-flight coroutine (freezing the demo on
 * whatever frame it was mid-storyboard) instead of trying to suspend/resume mid-sequence — a
 * flipped-back-to-false re-key just restarts the whole storyboard from its first frame, which
 * reads fine for a short looping demo like these.
 */
@Composable
private fun LaunchedEffectLoop(paused: Boolean, block: suspend () -> Unit) {
    LaunchedEffect(paused) {
        if (paused) return@LaunchedEffect
        while (true) {
            block()
        }
    }
}
