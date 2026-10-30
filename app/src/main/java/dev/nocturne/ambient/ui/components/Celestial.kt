package dev.nocturne.ambient.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MoonGlyph(
    phase: Float,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension * 0.46f
        val center = Offset(size.width / 2f, size.height / 2f)
        val circleRect = Rect(
            center.x - radius,
            center.y - radius,
            center.x + radius,
            center.y + radius
        )
        drawCircle(
            color = tint.copy(alpha = 0.08f),
            radius = radius,
            center = center
        )
        drawCircle(
            color = tint.copy(alpha = 0.30f),
            radius = radius,
            center = center,
            style = Stroke(width = (size.minDimension / 46f).coerceAtLeast(0.9f))
        )

        val angle = (phase.toDouble() * 2.0 * PI).toFloat()
        val k = cos(angle)
        val terminatorRect = Rect(
            center.x - radius * abs(k),
            center.y - radius,
            center.x + radius * abs(k),
            center.y + radius
        )
        val lit = Path()
        if (phase <= 0.5f) {
            lit.moveTo(center.x, center.y - radius)
            lit.arcTo(circleRect, -90f, 180f, false)
            lit.arcTo(terminatorRect, 90f, if (k >= 0f) -180f else 180f, false)
            lit.close()
        } else {
            lit.moveTo(center.x, center.y - radius)
            lit.arcTo(circleRect, -90f, -180f, false)
            lit.arcTo(terminatorRect, 90f, if (k <= 0f) -180f else 180f, false)
            lit.close()
        }
        drawPath(path = lit, color = tint)
    }
}

@Composable
fun SunArc(
    progress: Float,
    tint: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val baseline = size.height * 0.82f
        val radius = minOf(size.width * 0.42f, size.height * 0.72f)
        val cx = size.width / 2f
        val top = baseline - radius
        val rect = Rect(cx - radius, top, cx + radius, baseline + radius)

        drawArc(
            color = tint.copy(alpha = 0.22f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(
                width = (size.minDimension / 120f).coerceAtLeast(0.9f),
                cap = StrokeCap.Round
            )
        )
        drawArc(
            color = accent.copy(alpha = 0.50f),
            startAngle = 180f,
            sweepAngle = (180f * progress.coerceIn(0f, 1f)),
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(
                width = (size.minDimension / 120f).coerceAtLeast(0.9f),
                cap = StrokeCap.Round
            )
        )
        drawLine(
            color = tint.copy(alpha = 0.20f),
            start = Offset(cx - radius * 1.15f, baseline),
            end = Offset(cx + radius * 1.15f, baseline),
            strokeWidth = (size.minDimension / 220f).coerceAtLeast(0.6f)
        )
        val angle = PI.toFloat() * progress.coerceIn(0f, 1f)
        val sun = Offset(
            x = cx - cos(angle) * radius,
            y = baseline - sin(angle) * radius
        )
        drawCircle(
            color = accent.copy(alpha = 0.18f),
            radius = size.minDimension * 0.032f,
            center = sun
        )
        drawCircle(
            color = accent,
            radius = size.minDimension * 0.014f,
            center = sun
        )
    }
}
