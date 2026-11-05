package dev.nocturne.ambient.ui.faces

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nocturne.ambient.model.CalendarEvent
import dev.nocturne.ambient.model.Settings
import dev.nocturne.ambient.model.WeatherData
import dev.nocturne.ambient.ui.eventTime
import dev.nocturne.ambient.ui.formatClock
import dev.nocturne.ambient.ui.formattedDate
import dev.nocturne.ambient.ui.moonIllumination
import dev.nocturne.ambient.ui.moonName
import dev.nocturne.ambient.ui.moonPhaseFraction
import dev.nocturne.ambient.ui.nextEvent
import dev.nocturne.ambient.ui.temperature
import dev.nocturne.ambient.ui.theme.ClockSuffixType
import dev.nocturne.ambient.ui.theme.EyebrowType
import dev.nocturne.ambient.ui.theme.LabelType
import dev.nocturne.ambient.ui.theme.Palette
import dev.nocturne.ambient.ui.theme.Sora
import dev.nocturne.ambient.ui.timeParts
import dev.nocturne.ambient.ui.components.Eyebrow
import dev.nocturne.ambient.ui.components.Hairline
import dev.nocturne.ambient.ui.components.MoonGlyph
import dev.nocturne.ambient.ui.components.NextEventLine
import dev.nocturne.ambient.ui.components.SunTimes
import dev.nocturne.ambient.ui.components.WeatherGlyph
import java.time.LocalDateTime

@Composable
fun MinimalFace(
    settings: Settings,
    weather: WeatherData?,
    events: List<CalendarEvent>,
    now: LocalDateTime,
    palette: Palette,
    note: ByteArray? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth > 760.dp
        val timeSize = if (wide) 226.sp else 164.sp
        val dateSize = if (wide) 25.sp else 20.sp
        val parts = timeParts(now.toLocalTime(), settings.use24Hour)
        val event = nextEvent(events)
        val moonPhase = moonPhaseFraction(now.toLocalDate())
        val m = Modifier.padding(
            horizontal = if (wide) 64.dp else 40.dp,
            vertical = if (wide) 46.dp else 32.dp
        )

        Column(modifier = Modifier.fillMaxSize().then(m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(palette.accent)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "NOCTURNE",
                    style = EyebrowType.copy(letterSpacing = 3.2.sp),
                    color = palette.inkSoft
                )
                Spacer(Modifier.width(18.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(10.dp)
                        .background(palette.hairline)
                )
                Spacer(Modifier.width(18.dp))
                Text(
                    text = settings.locationLabel.uppercase(),
                    style = EyebrowType,
                    color = palette.inkFaint
                )
                Spacer(Modifier.weight(1f))
                MoonGlyph(
                    phase = moonPhase,
                    tint = palette.inkSoft,
                    background = palette.background,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "${moonName(moonPhase).uppercase()}  ·  ${moonIllumination(moonPhase)}%",
                    style = EyebrowType.copy(letterSpacing = 1.6.sp),
                    color = palette.inkFaint
                )
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedDate(now.toLocalDate()),
                        style = LabelType.copy(fontSize = dateSize, letterSpacing = 0.2.sp),
                        color = palette.inkSoft
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${parts.hour}:${parts.minute}",
                            fontFamily = Sora,
                            fontWeight = FontWeight.ExtraLight,
                            fontSize = timeSize,
                            lineHeight = timeSize,
                            letterSpacing = (-7).sp,
                            color = palette.ink,
                            maxLines = 1
                        )
                        if (settings.showSeconds) {
                            Column(
                                modifier = Modifier.padding(start = 18.dp, bottom = (timeSize.value * 0.12f).dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = parts.second,
                                    style = LabelType.copy(fontSize = 24.sp),
                                    color = palette.accent
                                )
                                if (parts.suffix.isNotEmpty()) {
                                    Text(
                                        text = parts.suffix,
                                        style = ClockSuffixType,
                                        color = palette.inkFaint
                                    )
                                }
                            }
                        } else if (parts.suffix.isNotEmpty()) {
                            Text(
                                text = parts.suffix,
                                style = ClockSuffixType,
                                color = palette.inkFaint,
                                modifier = Modifier.padding(start = 18.dp, bottom = (timeSize.value * 0.14f).dp)
                            )
                        }
                    }
                }
                if (wide && settings.noteEnabled && note != null) {
                    Spacer(Modifier.width(28.dp))
                    NoteCard(
                        note = note,
                        modifier = Modifier
                            .width(230.dp)
                            .height(300.dp)
                    )
                }
            }

            Spacer(Modifier.height(if (wide) 34.dp else 24.dp))
            Hairline(palette, alpha = 0.9f)
            Spacer(Modifier.height(if (wide) 22.dp else 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (settings.showWeather && weather != null) {
                    WeatherSummary(weather, settings, palette)
                } else if (settings.showWeather) {
                    Text(
                        text = "Weather unavailable",
                        style = LabelType,
                        color = palette.inkFaint
                    )
                }
                Spacer(Modifier.weight(1f))
                if (settings.showCalendar && event != null) {
                    NextEventLine(
                        event = event,
                        palette = palette,
                        use24Hour = settings.use24Hour,
                        modifier = Modifier.width(320.dp)
                    )
                }
                if (settings.showWeather && weather != null && weather.sunrise != null) {
                    Spacer(Modifier.width(34.dp))
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
private fun WeatherSummary(
    weather: WeatherData,
    settings: Settings,
    palette: Palette
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WeatherGlyph(
            kind = weather.kind,
            isDay = weather.isDay,
            tint = palette.inkSoft,
            accent = palette.accent,
            modifier = Modifier.size(31.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = temperature(weather.temperature, settings.celsius),
            style = LabelType.copy(fontSize = 24.sp, letterSpacing = 0.sp),
            color = palette.ink
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = weather.description,
                style = LabelType,
                color = palette.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "H ${temperature(weather.high, settings.celsius)}   L ${temperature(weather.low, settings.celsius)}" +
                    if (weather.stale) "   ·  CACHED" else "",
                style = EyebrowType.copy(letterSpacing = 1.2.sp),
                color = palette.inkFaint
            )
        }
    }
}

@Composable
private fun NoteCard(
    note: ByteArray,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(note) {
        BitmapFactory.decodeByteArray(note, 0, note.size)?.asImageBitmap()
    }
    if (bitmap == null) return
    Box(
        modifier = modifier
            .shadow(18.dp, RoundedCornerShape(7.dp))
            .clip(RoundedCornerShape(7.dp))
            .background(Color(0xFFF4F0E5))
            .border(1.dp, Color(0xFFD2CAB7), RoundedCornerShape(7.dp))
            .padding(12.dp)
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "Note from your phone",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}
