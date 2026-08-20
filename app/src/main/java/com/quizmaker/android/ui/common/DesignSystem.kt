package com.quizmaker.android.ui.common

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.BorderGray
import com.quizmaker.android.core.theme.BrandIndigo
import com.quizmaker.android.core.theme.BrandIndigoLight
import com.quizmaker.android.core.theme.GradientEnd
import com.quizmaker.android.core.theme.GradientStart
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.ScoreHighBg
import com.quizmaker.android.core.theme.ScoreLowBg
import com.quizmaker.android.core.theme.ScoreMidBg
import com.quizmaker.android.core.theme.SuccessGreen
import com.quizmaker.android.core.theme.SurfaceWhite
import com.quizmaker.android.core.theme.TextPrimary
import com.quizmaker.android.core.theme.TextSecondary

/**
 * Soft drop-shadow + clip + fill in one call — the "floating card" look used throughout
 * app.yunolms.com, as opposed to a flat `.clip().background()` with no depth.
 *
 * [color] defaults to [Color.Unspecified] rather than [SurfaceWhite] directly: SurfaceWhite is
 * now a @Composable-only, dark-theme-aware token (see Color.kt), and default parameter values on
 * a plain (non-@Composable) function like this one can't call it — `composed {}` below is what
 * gives us a @Composable context to resolve the real default in instead.
 */
fun Modifier.elevatedSurface(
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 8.dp,
    color: Color = Color.Unspecified
): Modifier = composed {
    val resolvedColor = if (color.isUnspecified) SurfaceWhite else color
    this
        .shadow(elevation = elevation, shape = shape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.16f))
        .clip(shape)
        .background(resolvedColor)
}

/** Shows `loadingContent` (a centered spinner by default) while `isLoading`, cross-fading into `content` once data arrives. */
@Composable
fun LoadingCrossfade(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandIndigo)
        }
    },
    content: @Composable () -> Unit
) {
    Crossfade(targetState = isLoading, modifier = modifier.fillMaxSize(), label = "loading-crossfade") { loading ->
        if (loading) {
            loadingContent()
        } else {
            content()
        }
    }
}

/** Friendly icon-in-a-circle empty state, replacing bare "No X yet" text. */
@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(BrandIndigoLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = BrandIndigo, modifier = Modifier.size(32.dp))
        }
        Box(Modifier.height(16.dp))
        Text(title, fontFamily = PoppinsFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Box(Modifier.height(4.dp))
            Text(subtitle, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

/** Pill-shaped, violet-to-pink gradient button used for the app's primary CTAs. */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 52.dp
) {
    val background = if (enabled) {
        Modifier.background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
    } else {
        // A flat muted fill reads clearly as "disabled" — alpha-dimming the gradient instead made
        // the purple/pink wash out toward pale blue/white, which looked like a rendering glitch.
        Modifier.background(BorderGray)
    }
    Box(
        modifier = modifier
            .height(height)
            .then(
                if (enabled) {
                    Modifier.shadow(elevation = 10.dp, shape = RoundedCornerShape(50), ambientColor = GradientStart.copy(alpha = 0.35f), spotColor = GradientEnd.copy(alpha = 0.45f))
                } else {
                    Modifier
                }
            )
            .clip(RoundedCornerShape(50))
            .then(background)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, tint = if (enabled) Color.White else TextSecondary, modifier = Modifier.size(20.dp))
                    Box(Modifier.size(8.dp))
                }
                Text(text, color = if (enabled) Color.White else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = PoppinsFamily)
            }
        }
    }
}

/** Dashboard stat card: colored icon chip, uppercase label, big bold value, green delta. */
@Composable
fun StatTile(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    delta: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Box(Modifier.height(8.dp))
            Text(
                text = label.uppercase(),
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(Modifier.height(2.dp))
            Text(value, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PoppinsFamily)
            if (delta != null) {
                Box(Modifier.height(4.dp))
                Text(delta, color = SuccessGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** Solid-filled pill badge (e.g. question type badges, selected filters). */
@Composable
fun FilledPill(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = com.quizmaker.android.core.theme.BrandIndigo,
    contentColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(text, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** Outlined pill badge (e.g. tags, difficulty labels). */
@Composable
fun OutlinedPill(
    text: String,
    modifier: Modifier = Modifier,
    borderColor: Color = BorderGray,
    contentColor: Color = TextPrimary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(text, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** Green/amber/red score pill used on the Leaderboard, banded by percentage. */
fun scoreBandColor(percent: Int): Color = when {
    percent >= 70 -> ScoreHighBg
    percent >= 40 -> ScoreMidBg
    else -> ScoreLowBg
}

@Composable
fun ScorePill(percent: Int, modifier: Modifier = Modifier) {
    FilledPill(text = "$percent%", modifier = modifier, containerColor = scoreBandColor(percent), contentColor = Color.White)
}

/** Selectable question-type card (used by both the Question Bank and Create Quiz "new question" sheets). */
@Composable
fun QuestionTypeOption(label: String, subtitle: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val border = if (selected) BrandIndigo else BorderGray
    val bg = if (selected) BrandIndigo.copy(alpha = 0.08f) else SurfaceWhite
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (selected) BrandIndigo else TextPrimary)
        Text(subtitle, fontSize = 12.sp, color = TextSecondary)
    }
}

/** A settings-list row: icon, label, trailing content (switch/chevron/value), optional bottom divider. */
@Composable
fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            if (subtitle != null) {
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
        trailing()
    }
}
