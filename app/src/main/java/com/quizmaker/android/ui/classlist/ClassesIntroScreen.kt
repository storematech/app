package com.quizmaker.android.ui.classlist

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Class
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AiCardBg
import com.quizmaker.android.core.theme.AiCardBorder
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary
import com.quizmaker.android.ui.common.GradientButton
import com.quizmaker.android.ui.common.elevatedSurface
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One-time "what Classes can do" interstitial shown the first time an account opens
 * More → Classes — see ClassesIntroViewModel.shouldShowIntro() for the once-per-account+device
 * gating, mirroring NotificationPermissionScreen's pattern.
 */
@Composable
fun ClassesIntroScreen(
    onAddClasses: () -> Unit,
    viewModel: ClassesIntroViewModel = hiltViewModel()
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
            ClassesIntroAnimation()
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "Group quizzes into Classes",
            textAlign = TextAlign.Center,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            color = TextPrimary
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Link quizzes to a Class to track combined performance, then generate an instant Class AI Summary of how the group is doing.",
            textAlign = TextAlign.Center,
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(32.dp))
        GradientButton(
            text = "Add Classes",
            onClick = {
                viewModel.markIntroShown(used = true)
                onAddClasses()
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
                onAddClasses()
            }
        )
        Spacer(Modifier.height(24.dp))
    }
}

private enum class ClassIntroPhase { ASSIGNING, SUMMARIZING }

/**
 * Two-scene looping story, swapped with a [Crossfade] inside a fixed-height area so the hero card
 * never resizes between scenes: first five quizzes fly into a Class, then a mock "Class AI Summary"
 * tap generates a summary — showing what a Class actually does rather than just describing it.
 */
@Composable
private fun ClassesIntroAnimation(modifier: Modifier = Modifier) {
    var phase by remember { mutableStateOf(ClassIntroPhase.ASSIGNING) }

    Box(modifier = modifier.fillMaxWidth().height(190.dp)) {
        Crossfade(targetState = phase, label = "classIntroPhase") { current ->
            when (current) {
                ClassIntroPhase.ASSIGNING -> QuizzesIntoClassScene(
                    onComplete = { phase = ClassIntroPhase.SUMMARIZING },
                    modifier = Modifier.fillMaxSize()
                )
                ClassIntroPhase.SUMMARIZING -> ClassAiSummaryScene(
                    onComplete = { phase = ClassIntroPhase.ASSIGNING },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/** Five quiz chips travel quickly, one after another, into a Class bubble whose badge ticks up to 5. */
@Composable
private fun QuizzesIntoClassScene(onComplete: () -> Unit, modifier: Modifier = Modifier) {
    val quizCount = 5
    val chipProgress = remember { Animatable(0f) }
    val chipScale = remember { Animatable(0f) }
    val classScale = remember { Animatable(1f) }
    var activeIndex by remember { mutableIntStateOf(0) }
    var badgeCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        for (index in 0 until quizCount) {
            activeIndex = index
            chipProgress.snapTo(0f)
            chipScale.snapTo(0f)

            chipScale.animateTo(1f, tween(110, easing = FastOutSlowInEasing))
            chipProgress.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
            chipScale.animateTo(0f, tween(90))

            coroutineScope {
                launch {
                    classScale.animateTo(1.18f, tween(90, easing = FastOutSlowInEasing))
                    classScale.animateTo(1f, tween(120, easing = FastOutSlowInEasing))
                }
            }
            badgeCount = index + 1
            delay(70)
        }
        delay(700)
        onComplete()
    }

    BoxWithConstraints(modifier = modifier.padding(horizontal = 24.dp)) {
        val chipSize = 34.dp
        val classSize = 64.dp
        val lineColor = BorderGray

        Canvas(modifier = Modifier.fillMaxSize()) {
            val endPoint = Offset(size.width / 2f, size.height - classSize.toPx() / 2f)
            (0 until quizCount).forEach { index ->
                val startPoint = Offset(size.width * ((index + 0.5f) / quizCount), chipSize.toPx() / 2f)
                drawLine(
                    color = lineColor,
                    start = startPoint,
                    end = endPoint,
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f))
                )
            }
        }

        repeat(quizCount) { index ->
            val centerXFraction = (index + 0.5f) / quizCount
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = maxWidth * centerXFraction - chipSize / 2, y = 0.dp)
                    .size(chipSize)
                    .clip(CircleShape)
                    .background(BrandIndigoLight),
                contentAlignment = Alignment.Center
            ) {
                Text((index + 1).toString(), color = BrandIndigo, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(classSize)
                    .scale(classScale.value)
                    .clip(CircleShape)
                    .background(BrandIndigoLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Class, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(28.dp))
            }
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(badgeCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (chipScale.value > 0f) {
            val startXFraction = (activeIndex + 0.5f) / quizCount
            val startX = maxWidth * startXFraction - chipSize / 2
            val endX = maxWidth / 2 - chipSize / 2
            val endY = maxHeight - classSize / 2 - chipSize / 2
            Box(
                modifier = Modifier
                    .offset(
                        x = lerp(startX, endX, chipProgress.value),
                        y = lerp(0.dp, endY, chipProgress.value)
                    )
                    .size(chipSize)
                    .scale(chipScale.value)
                    .clip(CircleShape)
                    .background(BrandIndigo),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** A mock tap on the Class AI Summary button generates a short summary, line by line, then confirms it's ready. */
@Composable
private fun ClassAiSummaryScene(onComplete: () -> Unit, modifier: Modifier = Modifier) {
    val tapScale = remember { Animatable(1f) }
    val cardAlpha = remember { Animatable(0f) }
    val lineProgress = remember { List(3) { Animatable(0f) } }
    val checkScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(200)
        tapScale.animateTo(0.82f, tween(110, easing = FastOutSlowInEasing))
        tapScale.animateTo(1f, tween(150, easing = FastOutSlowInEasing))

        cardAlpha.animateTo(1f, tween(220))
        delay(120)
        for (line in lineProgress) {
            line.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            delay(140)
        }

        checkScale.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
        delay(1100)
        onComplete()
    }

    Box(modifier = modifier.padding(horizontal = 24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .scale(tapScale.value)
                    .clip(CircleShape)
                    .background(AiCardBg)
                    .border(1.dp, AiCardBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text("Class AI Summary", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(cardAlpha.value)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AiCardBg)
                    .border(1.dp, AiCardBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lineProgress.forEachIndexed { index, anim ->
                        val fullWidthFraction = if (index == lineProgress.lastIndex) 0.6f else 1f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(anim.value * fullWidthFraction)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AiCardBorder)
                        )
                    }
                }
            }
            if (checkScale.value > 0f) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.scale(checkScale.value)) {
                    Box(
                        modifier = Modifier.size(16.dp).clip(CircleShape).background(SuccessGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Summary ready", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
