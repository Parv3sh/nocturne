package dev.nocturne.ambient.ui.faces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nocturne.ambient.model.CalendarEvent
import dev.nocturne.ambient.model.Settings
import dev.nocturne.ambient.model.WeatherData
import dev.nocturne.ambient.ui.formatClock
import dev.nocturne.ambient.ui.formattedDate
import dev.nocturne.ambient.ui.nextEvent
import dev.nocturne.ambient.ui.sunProgress
import dev.nocturne.ambient.ui.temperature
import dev.nocturne.ambient.ui.theme.ClockSuffixType
import dev.nocturne.ambient.ui.theme.EyebrowType
import dev.nocturne.ambient.ui.theme.LabelType
import dev.nocturne.ambient.ui.theme.Palette
import dev.nocturne.ambient.ui.theme.Sora
import dev.nocturne.ambient.ui.timeParts
import dev.nocturne.ambient.ui.eventTime
import dev.nocturne.ambient.ui.components.Eyebrow
import dev.nocturne.ambient.ui.components.Hairline
import dev.nocturne.ambient.ui.components.SunArc
import dev.nocturne.ambient.ui.components.SunTimes
import dev.nocturne.ambient.ui.components.WeatherGlyph
import dev.nocturne.ambient.ui.components.WorldClocks
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogFace(
    settings: Settings,
    weather: WeatherData?,
    events: List<CalendarEvent>,
    now: LocalDateTime,
    palette: Palette,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth > 760.dp
        val margin = if (wide) 56.dp else 32.dp
        val parts = timeParts(now.toLocalTime(), settings.use24Hour)
        val event = nextEvent(events)
        val progress = sunProgress(now, weather?.sunrise, weather?.sunset)

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
                Spacer(Modifier.weight(1f))
                if (settings.showWeather && weather?.aqi != null) {
                    Text(
                        text = "AQI ${weather.aqi}",
                        style = EyebrowType.copy(letterSpacing = 1.2.sp),
                        color = palette.inkFaint
                    )
                }
            }

            Spacer(Modifier.height(if (wide) 12.dp else 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.02f)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnalogDial(
                        now = now,
                        palette = palette,
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                    )
                }

                Spacer(Modifier.width(if (wide) 56.dp else 28.dp))

                Column(
                    modifier = Modifier.weight(0.98f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Eyebrow("Local time", palette)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${parts.hour}:${parts.minute}",
                            fontFamily = Sora,
                            fontWeight = FontWeight.ExtraLight,
                            fontSize = 72.sp,
                            lineHeight = 74.sp,
                            letterSpacing = (-2).sp,
                            color = palette.ink
                        )
                        if (parts.suffix.isNotEmpty()) {
                            Text(
                                text = parts.suffix,
                                style = ClockSuffixType,
                                color = palette.inkFaint,
                                modifier = Modifier.padding(start = 10.dp, bottom = 14.dp)
                            )
                        }
                    }
                    Text(
                        text = formattedDate(now.toLocalDate()),
                        style = LabelType.copy(fontSize = 21.sp),
                        color = palette.inkSoft
                    )

                    Spacer(Modifier.height(16.dp))
                    Hairline(palette)
                    Spacer(Modifier.height(14.dp))

                    if (settings.showWeather && weather != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            WeatherGlyph(
                                kind = weather.kind,
                                isDay = weather.isDay,
                                tint = palette.inkSoft,
                                accent = palette.accent,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = temperature(weather.temperature, settings.celsius),
                                style = LabelType.copy(fontSize = 30.sp),
                                color = palette.ink
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = weather.description,
                                    style = LabelType,
                                    color = palette.inkSoft
                                )
                                Text(
                                    text = "FEELS ${temperature(weather.apparent, settings.celsius)}   ·   " +
                                        "H ${temperature(weather.high, settings.celsius)}  L ${temperature(weather.low, settings.celsius)}",
                                    style = EyebrowType.copy(letterSpacing = 1.1.sp),
                                    color = palette.inkFaint
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    if (settings.showCalendar && event != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Eyebrow("Next", palette)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = event.title,
                                style = LabelType,
                                color = palette.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = eventTime(event, settings.use24Hour),
                                style = LabelType,
                                color = palette.accent
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    if (settings.showWeather && weather?.sunrise != null) {
                        SunArc(
                            progress = progress,
                            tint = palette.inkSoft,
                            accent = palette.accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RISE ${formatClock(weather.sunrise, settings.use24Hour)}",
                                style = EyebrowType,
                                color = palette.inkFaint
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "SET ${weather.sunset?.let { formatClock(it, settings.use24Hour) } ?: "—"}",
                                style = EyebrowType,
                                color = palette.inkFaint
                            )
                        }
                    }

                    if (settings.showWorldClocks) {
                        Spacer(Modifier.height(16.dp))
                        Hairline(palette, alpha = 0.8f)
                        Spacer(Modifier.height(14.dp))
                        WorldClocks(palette, settings, now)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalogDial(
    now: LocalDateTime,
    palette: Palette,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.44f
        val thin = (size.minDimension / 440f).coerceAtLeast(0.8f)

        drawCircle(
            color = palette.hairline.copy(alpha = 0.95f),
            radius = radius,
            center = center,
            style = Stroke(width = thin)
        )
        drawCircle(
            color = palette.ink.copy(alpha = 0.035f),
            radius = radius * 0.985f,
            center = center
        )

        for (i in 0 until 60) {
            val angle = (i * 6.0 - 90.0) * PI / 180.0
            val major = i % 5 == 0
            val outer = radius * 0.945f
            val inner = radius * (if (major) 0.895f else 0.925f)
            drawLine(
                color = if (major) palette.inkSoft.copy(alpha = 0.85f)
                else palette.inkFaint.copy(alpha = 0.45f),
                start = Offset(
                    center.x + cos(angle).toFloat() * outer,
                    center.y + sin(angle).toFloat() * outer
                ),
                end = Offset(
                    center.x + cos(angle).toFloat() * inner,
                    center.y + sin(angle).toFloat() * inner
                ),
                strokeWidth = if (major) thin * 1.8f else thin,
                cap = StrokeCap.Round
            )
        }

        val minute = now.minute + now.second / 60f
        val hour = (now.hour % 12) + minute / 60f
        val second = now.second

        rotate(degrees = hour * 30f, pivot = center) {
            drawLine(
                color = palette.ink,
                start = center,
                end = Offset(center.x, center.y - radius * 0.52f),
                strokeWidth = thin * 5.2f,
                cap = StrokeCap.Round
            )
        }
        rotate(degrees = minute * 6f, pivot = center) {
            drawLine(
                color = palette.ink.copy(alpha = 0.92f),
                start = center,
                end = Offset(center.x, center.y - radius * 0.74f),
                strokeWidth = thin * 2.4f,
                cap = StrokeCap.Round
            )
        }
        rotate(degrees = second * 6f, pivot = center) {
            drawLine(
                color = palette.accent,
                start = Offset(center.x, center.y + radius * 0.14f),
                end = Offset(center.x, center.y - radius * 0.84f),
                strokeWidth = thin * 1.1f,
                cap = StrokeCap.Round
            )
        }
        drawCircle(color = palette.background, radius = thin * 5f, center = center)
        drawCircle(color = palette.accent, radius = thin * 2.8f, center = center)
        drawCircle(
            color = palette.inkFaint.copy(alpha = 0.4f),
            radius = thin * 1.2f,
            center = center
        )
    }
}
