package dev.nocturne.ambient.ui.faces

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nocturne.ambient.model.CalendarEvent
import dev.nocturne.ambient.model.Settings
import dev.nocturne.ambient.model.WeatherData
import dev.nocturne.ambient.model.aqiLabel
import dev.nocturne.ambient.ui.dayLabel
import dev.nocturne.ambient.ui.eventTime
import dev.nocturne.ambient.ui.formatClock
import dev.nocturne.ambient.ui.formattedDate
import dev.nocturne.ambient.ui.greeting
import dev.nocturne.ambient.ui.isDaytime
import dev.nocturne.ambient.ui.moonIllumination
import dev.nocturne.ambient.ui.moonName
import dev.nocturne.ambient.ui.moonPhaseFraction
import dev.nocturne.ambient.ui.nextEvent
import dev.nocturne.ambient.ui.sunProgress
import dev.nocturne.ambient.ui.temperature
import dev.nocturne.ambient.ui.theme.EyebrowType
import dev.nocturne.ambient.ui.theme.LabelType
import dev.nocturne.ambient.ui.theme.Palette
import dev.nocturne.ambient.ui.theme.Sora
import dev.nocturne.ambient.ui.timeParts
import dev.nocturne.ambient.ui.components.Eyebrow
import dev.nocturne.ambient.ui.components.Hairline
import dev.nocturne.ambient.ui.components.MoonGlyph
import dev.nocturne.ambient.ui.components.SunArc
import dev.nocturne.ambient.ui.components.WeatherGlyph
import dev.nocturne.ambient.ui.components.WorldClocks
import java.time.LocalDateTime
import kotlin.math.roundToInt

@Composable
fun DashboardFace(
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
        val moonPhase = moonPhaseFraction(now.toLocalDate())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = margin, vertical = if (wide) 34.dp else 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Eyebrow(
                        text = "${settings.locationLabel}  ·  ${greeting(now.hour)}",
                        palette = palette
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${parts.hour}:${parts.minute}",
                            fontFamily = Sora,
                            fontWeight = FontWeight.ExtraLight,
                            fontSize = 62.sp,
                            lineHeight = 64.sp,
                            letterSpacing = (-2).sp,
                            color = palette.ink
                        )
                        if (parts.suffix.isNotEmpty()) {
                            Text(
                                text = parts.suffix,
                                style = EyebrowType,
                                color = palette.inkFaint,
                                modifier = Modifier.padding(start = 8.dp, bottom = 11.dp)
                            )
                        }
                    }
                    Text(
                        text = formattedDate(now.toLocalDate()),
                        style = LabelType.copy(fontSize = 18.sp),
                        color = palette.inkSoft
                    )
                }

                Spacer(Modifier.weight(1f))

                if (settings.showWeather && weather != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WeatherGlyph(
                            kind = weather.kind,
                            isDay = isDaytime(now, weather.sunrise, weather.sunset),
                            tint = palette.inkSoft,
                            accent = palette.accent,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = temperature(weather.temperature, settings.celsius),
                                    fontFamily = Sora,
                                    fontWeight = FontWeight.Light,
                                    fontSize = 48.sp,
                                    lineHeight = 50.sp,
                                    color = palette.ink
                                )
                                Text(
                                    text = weather.description,
                                    style = LabelType,
                                    color = palette.inkSoft,
                                    modifier = Modifier.padding(start = 10.dp, bottom = 8.dp)
                                )
                            }
                            Text(
                                text = "H ${temperature(weather.high, settings.celsius)}   L ${temperature(weather.low, settings.celsius)}" +
                                    "   ·   FEELS ${temperature(weather.apparent, settings.celsius)}" +
                                    if (weather.stale) "   ·   CACHED" else "",
                                style = EyebrowType.copy(letterSpacing = 1.1.sp),
                                color = palette.inkFaint
                            )
                        }
                    }
                } else if (settings.showWeather) {
                    Text("Weather offline", style = LabelType, color = palette.inkFaint)
                }
            }

            Spacer(Modifier.height(14.dp))
            Hairline(palette)
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1.08f).fillMaxHeight()) {
                    Eyebrow("7-day forecast", palette)
                    Spacer(Modifier.height(8.dp))
                    if (settings.showWeather && weather != null && weather.daily.isNotEmpty()) {
                        ForecastList(weather, settings, palette, now)
                    } else if (settings.showWeather) {
                        Text(
                            text = "Forecast unavailable",
                            style = LabelType,
                            color = palette.inkFaint
                        )
                    }
                }

                Spacer(Modifier.width(28.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(palette.hairline)
                )
                Spacer(Modifier.width(30.dp))

                Column(modifier = Modifier.weight(0.92f).fillMaxHeight()) {
                    Eyebrow("Agenda", palette)
                    Spacer(Modifier.height(8.dp))
                    if (settings.showCalendar && events.isNotEmpty()) {
                        events.take(3).forEach { item ->
                            AgendaRow(item, settings, palette)
                            Spacer(Modifier.height(9.dp))
                        }
                    } else {
                        Text(
                            text = if (settings.showCalendar) "No events coming up" else "Calendar hidden",
                            style = LabelType,
                            color = palette.inkFaint
                        )
                        if (settings.showCalendar && settings.calendarUrls.isBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Add an iCal feed in settings",
                                style = EyebrowType,
                                color = palette.inkFaint.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    if (settings.showWeather && weather?.sunrise != null) {
                        SunArc(
                            progress = sunProgress(now, weather.sunrise, weather.sunset),
                            tint = palette.inkSoft,
                            accent = palette.accent,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
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
                        Spacer(Modifier.height(10.dp))
                    }

                    Hairline(palette, alpha = 0.8f)
                    Spacer(Modifier.height(11.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (settings.showWorldClocks) {
                            WorldClocks(palette, settings)
                        }
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            val aqi = weather?.aqi
                            if (aqi != null) {
                                Text(
                                    text = "AQI $aqi  ·  ${aqiLabel(aqi)?.uppercase() ?: ""}",
                                    style = EyebrowType.copy(letterSpacing = 1.2.sp),
                                    color = palette.inkFaint
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MoonGlyph(
                                    phase = moonPhase,
                                    tint = palette.inkSoft,
                                    background = palette.background,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    text = "${moonName(moonPhase).uppercase()}  ${moonIllumination(moonPhase)}%",
                                    style = EyebrowType.copy(letterSpacing = 0.9.sp),
                                    color = palette.inkFaint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastList(
    weather: WeatherData,
    settings: Settings,
    palette: Palette,
    now: LocalDateTime
) {
    val days = weather.daily.take(7)
    val weekLow = days.minOfOrNull { it.low } ?: 0.0
    val weekHigh = days.maxOfOrNull { it.high } ?: 1.0
    val span = (weekHigh - weekLow).coerceAtLeast(1.0)

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        days.forEach { day ->
            val today = day.date == now.toLocalDate()
            Row(
                modifier = Modifier.fillMaxWidth().height(31.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayLabel(day.date, now.toLocalDate()),
                    style = EyebrowType.copy(
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp
                    ),
                    color = if (today) palette.accent else palette.inkSoft,
                    modifier = Modifier.width(76.dp)
                )
                WeatherGlyph(
                    kind = day.kind,
                    isDay = true,
                    tint = if (today) palette.ink else palette.inkSoft,
                    accent = palette.accent,
                    modifier = Modifier.size(23.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (day.precipitation > 0) "${day.precipitation}%" else "—",
                    style = EyebrowType.copy(fontSize = 9.sp, letterSpacing = 0.8.sp),
                    color = if (day.precipitation >= 40) palette.accent else palette.inkFaint,
                    modifier = Modifier.width(34.dp)
                )
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                ) {
                    val y = size.height / 2f
                    drawLine(
                        color = palette.hairline,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.2f,
                        cap = StrokeCap.Round
                    )
                    val start = ((day.low - weekLow) / span * size.width).toFloat()
                    val end = ((day.high - weekLow) / span * size.width).toFloat()
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(palette.accentSoft, palette.accent),
                            startX = start,
                            endX = end.coerceAtLeast(start + 1f)
                        ),
                        start = Offset(start, y),
                        end = Offset(end.coerceAtLeast(start + 1f), y),
                        strokeWidth = 3.2f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = palette.accent,
                        radius = 2.6f,
                        center = Offset(end.coerceAtLeast(start + 1f), y)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${temperature(day.low, settings.celsius).replace("°", "")}°",
                    style = LabelType.copy(fontSize = 12.sp),
                    color = palette.inkFaint,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(34.dp)
                )
                Text(
                    text = "${temperature(day.high, settings.celsius).replace("°", "")}°",
                    style = LabelType.copy(fontSize = 13.sp),
                    color = if (today) palette.ink else palette.inkSoft,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(38.dp)
                )
            }
        }
    }
}

@Composable
private fun AgendaRow(event: CalendarEvent, settings: Settings, palette: Palette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(24.dp)
                .background(palette.accent.copy(alpha = 0.7f))
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = LabelType.copy(fontSize = 13.sp),
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    if (!event.allDay && event.start.toLocalDate() != java.time.LocalDate.now()) {
                        append(event.start.format(java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.getDefault())).uppercase())
                        append("  ·  ")
                    }
                    append(eventTime(event, settings.use24Hour))
                    if (!event.allDay && !event.end.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
                        append(" – ")
                        append(formatClock(event.end.toLocalTime(), settings.use24Hour))
                    }
                },
                style = EyebrowType.copy(fontSize = 9.sp, letterSpacing = 0.9.sp),
                color = palette.inkFaint
            )
        }
    }
}
