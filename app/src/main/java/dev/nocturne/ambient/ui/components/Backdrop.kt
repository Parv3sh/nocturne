package dev.nocturne.ambient.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.nocturne.ambient.ui.theme.Palette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val alpha: Float,
    val phase: Float,
    val drift: Float
)

@Composable
fun NocturneBackdrop(
    palette: Palette,
    ambient: Boolean = false,
    motion: Boolean = true,
    modifier: Modifier = Modifier
) {
    val stars = remember {
        val random = Random(23)
        List(92) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat() * 0.78f,
                radius = 0.35f + random.nextFloat() * 1.15f,
                alpha = 0.12f + random.nextFloat() * 0.55f,
                phase = random.nextFloat() * (2f * PI.toFloat()),
                drift = 0.000012f + random.nextFloat() * 0.000025f
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "backdrop")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(120_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "star-phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(palette.backgroundDeep, palette.background, palette.backgroundDeep),
                startY = 0f,
                endY = size.height
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(palette.accent.copy(alpha = 0.085f), Color.Transparent),
                center = Offset(size.width * 0.78f, size.height * 0.12f),
                radius = size.width * 0.62f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(palette.accentSoft.copy(alpha = 0.045f), Color.Transparent),
                center = Offset(size.width * 0.12f, size.height * 0.92f),
                radius = size.width * 0.55f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f)),
                center = Offset(size.width * 0.5f, size.height * 0.48f),
                radius = size.width * 0.78f
            )
        )
        if (ambient) {
            val time = if (motion) phase.value else 0f
            for (star in stars) {
                val twinkle = 0.5f + 0.5f * sin(time + star.phase)
                val x = ((star.x + time * star.drift) % 1f) * size.width
                val radius = star.radius * (size.minDimension / 720f).coerceAtLeast(0.7f)
                drawCircle(
                    color = palette.ink.copy(alpha = star.alpha * (0.35f + 0.65f * twinkle)),
                    radius = radius,
                    center = Offset(x, star.y * size.height)
                )
            }
            drawSoftHorizon(palette)
        }
    }
}

private fun DrawScope.drawSoftHorizon(palette: Palette) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, palette.accent.copy(alpha = 0.045f), Color.Transparent),
            startY = size.height * 0.62f,
            endY = size.height * 0.86f
        ),
        topLeft = Offset(0f, size.height * 0.62f),
        size = Size(size.width, size.height * 0.24f)
    )
}

@Composable
fun Vignette(modifier: Modifier = Modifier, strength: Float = 0.24f) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = strength)),
                    radius = 1400f
                )
            )
    )
}
