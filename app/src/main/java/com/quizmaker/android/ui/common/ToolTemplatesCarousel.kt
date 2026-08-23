package com.quizmaker.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizmaker.android.core.theme.PoppinsFamily
import com.quizmaker.android.core.theme.TextPrimary

/**
 * "Templates" carousel shown on each Tool's list screen (Onboarding/Feedback/Poll/Voting/RSVP) —
 * same "colored gradient card, watermark icon, white bold label" recipe as the AI Quiz screen's
 * "Trending Templates" carousel (ui/aiquiz/AiQuizScreen.kt's TemplateChip), reused verbatim here
 * rather than approximated, per request to keep the two feeling like the same feature. That
 * carousel colors cards by a fixed prompt *category* (history/science/math/...); these templates
 * don't have an equivalent grouping, so the gradient instead just cycles through the same palette
 * by position — each card still gets its own template-specific icon as both the watermark and the
 * small badge, exactly like AI's per-category icon does there.
 */
@Composable
fun <T> ToolTemplatesCarousel(
    templates: List<T>,
    icon: (T) -> ImageVector,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "Templates",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(templates.size) { index ->
                val template = templates[index]
                ToolTemplateChip(
                    icon = icon(template),
                    label = label(template),
                    gradient = TEMPLATE_GRADIENTS[index % TEMPLATE_GRADIENTS.size],
                    onClick = { onSelect(template) }
                )
            }
        }
    }
}

private data class TemplateGradient(val start: Color, val end: Color)

/** Same eight colors AiQuizScreen's themeFor() cycles a prompt category through — reused here by
 *  position instead of category so the visual language matches without needing an equivalent to
 *  AiPromptCategory for every one of these tools. */
private val TEMPLATE_GRADIENTS = listOf(
    TemplateGradient(Color(0xFFB45309), Color(0xFF78350F)),
    TemplateGradient(Color(0xFF16A34A), Color(0xFF15803D)),
    TemplateGradient(Color(0xFF2563EB), Color(0xFF1D4ED8)),
    TemplateGradient(Color(0xFF9333EA), Color(0xFF6B21A8)),
    TemplateGradient(Color(0xFFDC2626), Color(0xFF991B1B)),
    TemplateGradient(Color(0xFF0D9488), Color(0xFF115E59)),
    TemplateGradient(Color(0xFFD97706), Color(0xFF92400E)),
    TemplateGradient(Color(0xFF4F46E5), Color(0xFF3730A3))
)

@Composable
private fun ToolTemplateChip(icon: ImageVector, label: String, gradient: TemplateGradient, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(gradient.start, gradient.end)))
            .clickable(onClick = onClick)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.16f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 16.dp, y = 16.dp)
                .size(64.dp)
                .rotate(-15f)
        )
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                label,
                color = Color.White,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
