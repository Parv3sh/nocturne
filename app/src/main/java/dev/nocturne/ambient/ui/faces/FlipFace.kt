package dev.nocturne.ambient.ui.faces

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nocturne.ambient.model.CalendarEvent
import dev.nocturne.ambient.model.Settings
import dev.nocturne.ambient.model.WeatherData
import dev.nocturne.ambient.ui.formatClock
import dev.nocturne.ambient.ui.formattedDate
import dev.nocturne.ambient.ui.nextEvent
import dev.nocturne.ambient.ui.temperature
import dev.nocturne.ambient.ui.theme.EyebrowType
import dev.nocturne.ambient.ui.theme.LabelType
import dev.nocturne.ambient.ui.theme.Palette
import dev.nocturne.ambient.ui.theme.Sora
import dev.nocturne.ambient.ui.timeParts
import dev.nocturne.ambient.ui.components.Eyebrow
import dev.nocturne.ambient.ui.components.NextEventLine
import dev.nocturne.ambient.ui.components.SunTimes
import dev.nocturne.ambient.ui.components.WeatherGlyph
import java.time.LocalDateTime

@Composable
fun FlipFace(
    settings: Settings,
    weather: WeatherData?,
    events: List<CalendarEvent>,
    now: LocalDateTime,
    palette: Palette,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth > 760.dp
        val parts = timeParts(now.toLocalTime(), settings.use24Hour)
        val event = nextEvent(events)
        val cardWidth = if (wide) 154.dp else 116.dp
        val cardHeight = if (wide) 218.dp else 166.dp
        val cardGap = if (wide) 12.dp else 8.dp
        val margin = if (wide) 56.dp else 32.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = margin, vertical = if (wide) 40.dp else 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(palette.accent)
                )
                Spacer(Modifier.width(12.dp))
                Text("NOCTURNE", style = EyebrowType.copy(letterSpacing = 3.2.sp), color = palette.inkSoft)
                Spacer(Modifier.width(18.dp))
                Text(
                    text = "${settings.locationLabel.uppercase()}  ·  ${formattedDate(now.toLocalDate()).uppercase()}",
                    style = EyebrowType,
                    color = palette.inkFaint
                )
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlipGroup(
                    label = "HOURS",
                    digits = parts.hour.padStart(2, '0'),
                    palette = palette,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    gap = cardGap
                )
                Spacer(Modifier.width(if (wide) 28.dp else 16.dp))
                FlipGroup(
                    label = "MINUTES",
                    digits = parts.minute,
                    palette = palette,
                    cardWidth = cardWidth,
                    cardHeight = cardHeight,
                    gap = cardGap
                )
                if (parts.suffix.isNotEmpty()) {
                    Spacer(Modifier.width(if (wide) 24.dp else 12.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = parts.suffix,
                            style = EyebrowType.copy(fontSize = 14.sp, letterSpacing = 2.sp),
                            color = palette.accent
                        )
                        if (settings.showSeconds) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = parts.second,
                                style = LabelType.copy(fontSize = 18.sp),
                                color = palette.inkFaint
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (settings.showWeather && weather != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WeatherGlyph(
                            kind = weather.kind,
                            isDay = weather.isDay,
                            tint = palette.inkSoft,
                            accent = palette.accent,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = temperature(weather.temperature, settings.celsius),
                            style = LabelType.copy(fontSize = 23.sp),
                            color = palette.ink
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            text = weather.description,
                            style = LabelType,
                            color = palette.inkSoft
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (settings.showCalendar && event != null) {
                    NextEventLine(
                        event = event,
                        palette = palette,
                        use24Hour = settings.use24Hour,
                        modifier = Modifier.width(310.dp)
                    )
                }
                if (settings.showWeather && weather != null && weather.sunrise != null) {
                    Spacer(Modifier.width(30.dp))
                    SunTimes(
                        sunrise = formatClock(weather.sunrise, settings.use24Hour),
                        sunset = weather.sunset?.let { formatClock(it, settings.use24Hour) } ?: "—",
                        palette = palette
                    )
                }
            }
        }
    }
}

@Composable
private fun FlipGroup(
    label: String,
    digits: String,
    palette: Palette,
    cardWidth: Dp,
    cardHeight: Dp,
    gap: Dp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            digits.forEach { digit ->
                FlipCard(
                    digit = digit,
                    palette = palette,
                    width = cardWidth,
                    height = cardHeight
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Eyebrow(label, palette)
    }
}

@Composable
private fun FlipCard(
    digit: Char,
    palette: Palette,
    width: Dp,
    height: Dp
) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape((width.value * 0.055f).dp))
            .drawBehind {
                val radius = CornerRadius(width.toPx() * 0.055f)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            palette.surface,
                            palette.backgroundDeep
                        )
                    ),
                    cornerRadius = radius
                )
                drawRoundRect(
                    color = palette.hairline.copy(alpha = 0.9f),
                    cornerRadius = radius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.62f),
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 2.4f
                )
                drawLine(
                    color = palette.ink.copy(alpha = 0.045f),
                    start = Offset(0f, size.height / 2f + 1.5f),
                    end = Offset(size.width, size.height / 2f + 1.5f),
                    strokeWidth = 1f
                )
                drawLine(
                    color = palette.ink.copy(alpha = 0.05f),
                    start = Offset(1f, 1f),
                    end = Offset(size.width - 1f, 1f),
                    strokeWidth = 1.5f
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = digit,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(260)) { fullHeight -> -fullHeight } +
                    fadeIn(animationSpec = tween(180))) togetherWith
                    (slideOutVertically(animationSpec = tween(260)) { fullHeight -> fullHeight } +
                        fadeOut(animationSpec = tween(140)))
            },
            label = "flip-digit"
        ) { value ->
            Text(
                text = value.toString(),
                fontFamily = Sora,
                fontWeight = FontWeight.ExtraLight,
                fontSize = (height.value * 0.67f).sp,
                lineHeight = (height.value * 0.67f).sp,
                color = palette.ink
            )
        }
    }
}
