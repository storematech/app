package com.quizmaker.android.ui.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.quizmaker.android.core.theme.AppBackground
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.ErrorRed
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
 * One-time "why we want this" interstitial shown right before the system notification permission
 * dialog — see SessionViewModel.resolvePostAuthGate() for exactly when (after phone collection for
 * a new account, or right after sign-in for a returning one, once per account+device, and skipped
 * entirely below API 33 / once already granted).
 */
@Composable
fun NotificationPermissionScreen(
    onContinue: () -> Unit,
    viewModel: NotificationPermissionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var requested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.markPromptShown(if (granted) NotificationPermissionOutcome.GRANTED else NotificationPermissionOutcome.DENIED)
        onContinue()
    }

    fun requestPermission() {
        if (requested) return
        requested = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.markPromptShown(NotificationPermissionOutcome.GRANTED)
            onContinue()
        } else {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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
            SubmissionNotificationAnimation()
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "Stay in the loop",
            textAlign = TextAlign.Center,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            color = TextPrimary
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "The moment a learner submits your quiz, we send you an alert — turn on notifications so you never miss one.",
            textAlign = TextAlign.Center,
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(32.dp))
        GradientButton(text = "Enable Notifications", onClick = ::requestPermission, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))
        Text(
            "Maybe later",
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.clickable {
                viewModel.markPromptShown(NotificationPermissionOutcome.SKIPPED)
                onContinue()
            }
        )
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Small looping scene instead of a static bell icon: a submission "travels" from the student side
 * to the teacher (you) side, where it lands as a bell bounce + a "New submission!" toast — showing
 * the actual thing notifications are for, rather than just telling the user in a sentence.
 */
@Composable
private fun SubmissionNotificationAnimation(modifier: Modifier = Modifier) {
    val paperProgress = remember { Animatable(0f) }
    val paperScale = remember { Animatable(0f) }
    val bellScale = remember { Animatable(1f) }
    val badgeScale = remember { Animatable(0f) }
    val toastAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            paperProgress.snapTo(0f)
            paperScale.snapTo(0f)
            badgeScale.snapTo(0f)
            toastAlpha.snapTo(0f)
            bellScale.snapTo(1f)

            // Submission appears at the student side, then travels across to the teacher side.
            paperScale.animateTo(1f, tween(250, easing = FastOutSlowInEasing))
            delay(150)
            paperProgress.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
            paperScale.animateTo(0f, tween(160))

            // Arrival: bell bounces, an unread badge pops in, and a toast confirms what happened.
            coroutineScope {
                launch {
                    bellScale.animateTo(1.3f, tween(150, easing = FastOutSlowInEasing))
                    bellScale.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
                }
                launch { badgeScale.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }
                launch { toastAlpha.animateTo(1f, tween(220)) }
            }
            delay(1300)
            toastAlpha.animateTo(0f, tween(250))
            delay(500)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 24.dp)
    ) {
        val bubbleSize = 56.dp
        val travel = maxWidth - bubbleSize
        // Resolved here, in @Composable context, rather than read inside Canvas's draw lambda
        // below — that lambda is a plain DrawScope.() -> Unit, not @Composable, and BorderGray is
        // now a @Composable, theme-aware token (see Color.kt).
        val lineColor = BorderGray

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.Center)
                .padding(horizontal = bubbleSize / 2)
        ) {
            drawLine(
                color = lineColor,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
        }

        PersonBubble(
            icon = Icons.Default.Person,
            label = "Learner submits",
            size = bubbleSize,
            circleColor = BrandIndigoLight,
            iconTint = BrandIndigo,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        Column(modifier = Modifier.align(Alignment.CenterEnd), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(bubbleSize)
                        .scale(bellScale.value)
                        .clip(CircleShape)
                        .background(SuccessGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(26.dp))
                }
                if (badgeScale.value > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .scale(badgeScale.value)
                            .clip(CircleShape)
                            .background(ErrorRed)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("You're notified", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }

        if (paperScale.value > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = travel * paperProgress.value)
                    .size(bubbleSize * 0.6f)
                    .scale(paperScale.value)
                    .clip(CircleShape)
                    .background(BrandIndigo),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Assignment, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        if (toastAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .alpha(toastAlpha.value)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SuccessGreen)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("New submission!", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PersonBubble(icon: ImageVector, label: String, size: Dp, circleColor: Color, iconTint: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.width(size + 20.dp))
    }
}
