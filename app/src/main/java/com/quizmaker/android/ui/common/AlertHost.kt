package com.quizmaker.android.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.alert.AlertBus
import com.quizmaker.android.core.alert.AlertType
import com.quizmaker.android.core.alert.AppAlert
import com.quizmaker.android.core.theme.ErrorRed
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.SuccessGreen
import kotlinx.coroutines.delay

private const val ALERT_DISPLAY_MS = 2800L

/**
 * Mounted once at the nav-graph root, floating above every screen — see NavGraph.kt. Collects
 * AlertBus (safeCall()'s errors + ViewModels' explicit success calls) and shows one premium strip
 * at a time; a second alert arriving mid-display simply queues behind the first via the SharedFlow
 * collector rather than interrupting it.
 */
@Composable
fun AlertHost(modifier: Modifier = Modifier) {
    var currentAlert by remember { mutableStateOf<AppAlert?>(null) }

    LaunchedEffect(Unit) {
        AlertBus.alerts.collect { alert ->
            currentAlert = alert
            delay(ALERT_DISPLAY_MS)
            currentAlert = null
        }
    }

    AnimatedVisibility(
        visible = currentAlert != null,
        enter = slideInVertically(tween(280)) { -it } + fadeIn(tween(280)),
        exit = slideOutVertically(tween(220)) { -it } + fadeOut(tween(220)),
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        currentAlert?.let { AlertStrip(it) }
    }
}

@Composable
private fun AlertStrip(alert: AppAlert) {
    val isSuccess = alert.type == AlertType.SUCCESS
    val backgroundColor = if (isSuccess) SuccessGreen else ErrorRed
    val icon = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(10.dp))
        Text(
            alert.message,
            color = Color.White,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
