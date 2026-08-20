package com.quizmaker.android.ui.learners

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
 * One-time "what Learners can do" interstitial shown the first time an account opens
 * More → Learners — see LearnersIntroViewModel.shouldShowIntro() for the once-per-account+device
 * gating, mirroring NotificationPermissionScreen's pattern.
 */
@Composable
fun LearnersIntroScreen(
    onAddLearners: () -> Unit,
    viewModel: LearnersIntroViewModel = hiltViewModel()
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
                .padding(vertical = 20.dp)
        ) {
            LearnersRosterAnimation()
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "Keep your roster in one place",
            textAlign = TextAlign.Center,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            color = TextPrimary
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Add learners once and use them everywhere — group them, track quiz history, and keep parent contact details all in one place.",
            textAlign = TextAlign.Center,
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(32.dp))
        GradientButton(
            text = "Add Learners",
            onClick = {
                viewModel.markIntroShown(used = true)
                onAddLearners()
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
                onAddLearners()
            }
        )
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Small looping scene: three roster rows fade/slide in one at a time, then a checkmark confirms
 * the roster is ready — showing what "adding learners" actually builds up, rather than just
 * describing it in a sentence.
 */
@Composable
private fun LearnersRosterAnimation(modifier: Modifier = Modifier) {
    val rowCount = 3
    val rowAlphas = remember { List(rowCount) { Animatable(0f) } }
    val rowOffsets = remember { List(rowCount) { Animatable(20f) } }
    val checkScale = remember { Animatable(0f) }
    val toastAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            rowAlphas.forEach { it.snapTo(0f) }
            rowOffsets.forEach { it.snapTo(20f) }
            checkScale.snapTo(0f)
            toastAlpha.snapTo(0f)

            for (index in 0 until rowCount) {
                coroutineScope {
                    launch { rowAlphas[index].animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }
                    launch { rowOffsets[index].animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
                }
                delay(280)
            }

            checkScale.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
            toastAlpha.animateTo(1f, tween(200))
            delay(1200)
            toastAlpha.animateTo(0f, tween(250))
            delay(500)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(rowCount) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(rowAlphas[index].value)
                        .offset(x = rowOffsets[index].value.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(BrandIndigoLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.width(96.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(BorderGray))
                        Box(Modifier.width(64.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(BorderGray))
                    }
                }
            }
        }

        if (checkScale.value > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .scale(checkScale.value)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
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
                Text("Roster ready!", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
