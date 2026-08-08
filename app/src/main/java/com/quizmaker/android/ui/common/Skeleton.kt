package com.quizmaker.android.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A softly animated left-to-right sweep, the standard "skeleton loading" shimmer. */
@Composable
private fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    return Brush.linearGradient(
        colors = listOf(Color(0xFFE8E8F1), Color(0xFFF4F4FA), Color(0xFFE8E8F1)),
        start = Offset(translate, 0f),
        end = Offset(translate + 400f, 400f)
    )
}

/** A single shimmering rounded-rect placeholder — the base building block for skeleton screens. */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(8.dp)) {
    Box(modifier = modifier.clip(shape).background(rememberShimmerBrush()))
}

/** A text-line-shaped shimmer, for mimicking a title/subtitle while content loads. */
@Composable
fun SkeletonLine(width: Dp, height: Dp = 14.dp, modifier: Modifier = Modifier) {
    SkeletonBox(modifier = modifier.width(width).height(height), shape = RoundedCornerShape(4.dp))
}

/** Row of shimmering stat-tile placeholders, matching [StatTile]'s footprint. */
@Composable
fun SkeletonStatTiles(count: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            SkeletonBox(
                modifier = Modifier.weight(1f).height(70.dp),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/** A shimmering card matching the "title + subtitle + detail row" shape most list rows share. */
@Composable
fun SkeletonCardRow(modifier: Modifier = Modifier, lineWidths: List<Dp> = listOf(160.dp, 220.dp)) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        lineWidths.forEachIndexed { index, width ->
            SkeletonLine(width = width, height = if (index == 0) 16.dp else 12.dp)
            if (index != lineWidths.lastIndex) Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Drop-in skeleton for the common screen shape used across this app: an optional row of stat
 * tiles up top, followed by a scrollable list of card-shaped rows.
 */
@Composable
fun ListScreenSkeleton(
    modifier: Modifier = Modifier,
    statTileCount: Int = 0,
    rowCount: Int = 5,
    rowLineWidths: List<Dp> = listOf(160.dp, 220.dp)
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (statTileCount > 0) {
            item { SkeletonStatTiles(count = statTileCount) }
        }
        items(rowCount) {
            SkeletonCardRow(lineWidths = rowLineWidths)
        }
    }
}
