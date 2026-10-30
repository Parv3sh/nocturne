package dev.nocturne.ambient.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.nocturne.ambient.model.WeatherKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WeatherGlyph(
    kind: WeatherKind,
    isDay: Boolean,
    tint: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    stroke: Float = 1.9f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokePx = stroke * (size.minDimension / 36f).coerceAtLeast(0.8f)
        val cloudColor = tint.copy(alpha = 0.9f)
        val sunColor = if (isDay) accent else tint.copy(alpha = 0.75f)

        fun sun(cx: Float, cy: Float, radius: Float, rays: Boolean = true) {
            if (!isDay) {
                drawCircle(color = sunColor, radius = radius, center = Offset(cx, cy), style = Stroke(strokePx))
                drawCircle(
                    color = tint.copy(alpha = 0.15f),
                    radius = radius * 0.55f,
                    center = Offset(cx + radius * 0.2f, cy - radius * 0.18f)
                )
                return
            }
            drawCircle(
                color = sunColor.copy(alpha = 0.16f),
                radius = radius * 1.45f,
                center = Offset(cx, cy)
            )
            drawCircle(color = sunColor, radius = radius, center = Offset(cx, cy), style = Stroke(strokePx))
            if (rays) {
                for (i in 0 until 8) {
                    val a = i * PI.toFloat() / 4f
                    val inner = radius * 1.55f
                    val outer = radius * 2.1f
                    drawLine(
                        color = sunColor,
                        start = Offset(cx + cos(a) * inner, cy + sin(a) * inner),
                        end = Offset(cx + cos(a) * outer, cy + sin(a) * outer),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        fun cloud(offsetX: Float = 0f, offsetY: Float = 0f, scale: Float = 1f) {
            val path = Path().apply {
                moveTo(w * 0.18f + offsetX, h * 0.70f + offsetY)
                cubicTo(
                    w * 0.08f + offsetX, h * 0.70f + offsetY,
                    w * 0.07f + offsetX, h * 0.55f + offsetY,
                    w * 0.20f + offsetX, h * 0.52f + offsetY
                )
                cubicTo(
                    w * 0.22f + offsetX, h * 0.34f + offsetY,
                    w * 0.47f + offsetX, h * 0.32f + offsetY,
                    w * 0.54f + offsetX, h * 0.47f + offsetY
                )
                cubicTo(
                    w * 0.67f + offsetX, h * 0.40f + offsetY,
                    w * 0.84f + offsetX, h * 0.49f + offsetY,
                    w * 0.82f + offsetX, h * 0.64f + offsetY
                )
                cubicTo(
                    w * 0.94f + offsetX, h * 0.69f + offsetY,
                    w * 0.91f + offsetX, h * 0.82f + offsetY,
                    w * 0.76f + offsetX, h * 0.82f + offsetY
                )
                lineTo(w * 0.31f + offsetX, h * 0.82f + offsetY)
                cubicTo(
                    w * 0.22f + offsetX, h * 0.82f + offsetY,
                    w * 0.15f + offsetX, h * 0.77f + offsetY,
                    w * 0.18f + offsetX, h * 0.70f + offsetY
                )
                close()
            }
            drawPath(
                path = path,
                color = cloudColor,
                style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        when (kind) {
            WeatherKind.CLEAR -> sun(w * 0.5f, h * 0.48f, w * 0.16f)
            WeatherKind.PARTLY -> {
                sun(w * 0.34f, h * 0.34f, w * 0.115f)
                cloud(offsetX = w * 0.05f, offsetY = h * 0.06f, scale = 0.92f)
            }
            WeatherKind.CLOUDY -> {
                cloud(offsetX = -w * 0.08f, offsetY = -h * 0.08f, scale = 0.82f)
                cloud(offsetX = w * 0.08f, offsetY = h * 0.04f, scale = 0.92f)
            }
            WeatherKind.FOG -> {
                cloud(offsetY = -h * 0.12f)
                drawLine(
                    color = tint.copy(alpha = 0.7f),
                    start = Offset(w * 0.18f, h * 0.84f),
                    end = Offset(w * 0.82f, h * 0.84f),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint.copy(alpha = 0.45f),
                    start = Offset(w * 0.28f, h * 0.93f),
                    end = Offset(w * 0.72f, h * 0.93f),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
            WeatherKind.DRIZZLE, WeatherKind.RAIN -> {
                cloud(offsetY = -h * 0.10f)
                val drops = if (kind == WeatherKind.RAIN) 4 else 2
                for (i in 0 until drops) {
                    val x = w * (0.33f + i * 0.12f)
                    drawLine(
                        color = accent,
                        start = Offset(x, h * 0.80f),
                        end = Offset(x - w * 0.025f, h * 0.92f),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }
            }
            WeatherKind.SNOW -> {
                cloud(offsetY = -h * 0.10f)
                for (i in 0 until 3) {
                    drawCircle(
                        color = accent,
                        radius = w * 0.018f,
                        center = Offset(w * (0.34f + i * 0.15f), h * 0.87f)
                    )
                }
            }
            WeatherKind.STORM -> {
                cloud(offsetY = -h * 0.08f)
                val bolt = Path().apply {
                    moveTo(w * 0.51f, h * 0.70f)
                    lineTo(w * 0.44f, h * 0.86f)
                    lineTo(w * 0.52f, h * 0.84f)
                    lineTo(w * 0.47f, h * 1.00f)
                    lineTo(w * 0.62f, h * 0.78f)
                    lineTo(w * 0.54f, h * 0.80f)
                    lineTo(w * 0.60f, h * 0.70f)
                    close()
                }
                drawPath(bolt, color = accent)
            }
        }
    }
}
