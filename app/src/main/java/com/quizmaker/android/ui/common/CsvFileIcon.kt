package com.quizmaker.android.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quizmaker.android.core.theme.SuccessGreen

/**
 * A document-shaped icon with a folded corner and a "CSV" label, standing in for the generic
 * grid glyph so CSV export actions read as an actual file type at a glance.
 */
@Composable
fun CsvFileIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = SuccessGreen
) {
    val labelPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            color = android.graphics.Color.WHITE
        }
    }
    Canvas(
        modifier = modifier
            .size(24.dp)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            }
    ) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.5.dp.toPx()

        val outline = Path().apply {
            moveTo(w * 0.18f, h * 0.05f)
            lineTo(w * 0.60f, h * 0.05f)
            lineTo(w * 0.86f, h * 0.31f)
            lineTo(w * 0.86f, h * 0.95f)
            lineTo(w * 0.18f, h * 0.95f)
            close()
        }
        drawPath(outline, color = tint, style = Stroke(width = strokeWidth))

        val fold = Path().apply {
            moveTo(w * 0.60f, h * 0.05f)
            lineTo(w * 0.60f, h * 0.31f)
            lineTo(w * 0.86f, h * 0.31f)
        }
        drawPath(fold, color = tint, style = Stroke(width = strokeWidth))

        val bannerTop = h * 0.58f
        val bannerBottom = h * 0.80f
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.18f, bannerTop),
            size = Size(w * 0.68f, bannerBottom - bannerTop)
        )

        labelPaint.textSize = (bannerBottom - bannerTop) * 0.82f
        drawContext.canvas.nativeCanvas.drawText(
            "CSV",
            w * 0.52f,
            bannerBottom - (bannerBottom - bannerTop) * 0.18f,
            labelPaint
        )
    }
}
