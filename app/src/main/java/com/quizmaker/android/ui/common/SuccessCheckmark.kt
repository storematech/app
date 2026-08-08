package com.quizmaker.android.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quizmaker.android.core.theme.SuccessGreen

/**
 * Round green badge with a white checkmark that draws itself on — the Paytm/GPay-style payment
 * or onboarding success moment. Pure Compose (Canvas + PathMeasure stroke-trim), no Lottie/GIF
 * dependency needed for a single reusable animation this simple.
 */
@Composable
fun SuccessCheckmark(modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val circleScale = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        circleScale.animateTo(1f, animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing))
        checkProgress.animateTo(1f, animationSpec = tween(durationMillis = 420, easing = LinearOutSlowInEasing))
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(circleScale.value)
            .clip(CircleShape)
            .background(SuccessGreen),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.55f)) {
            val w = this.size.width
            val h = this.size.height
            val path = Path().apply {
                moveTo(w * 0.20f, h * 0.52f)
                lineTo(w * 0.42f, h * 0.72f)
                lineTo(w * 0.82f, h * 0.28f)
            }

            val measure = PathMeasure()
            measure.setPath(path, false)
            val animatedPath = Path()
            measure.getSegment(0f, measure.length * checkProgress.value, animatedPath, true)

            drawPath(
                path = animatedPath,
                color = Color.White,
                style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
