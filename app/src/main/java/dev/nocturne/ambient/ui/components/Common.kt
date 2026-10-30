package dev.nocturne.ambient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nocturne.ambient.model.CalendarEvent
import dev.nocturne.ambient.model.Settings
import dev.nocturne.ambient.ui.eventTime
import dev.nocturne.ambient.ui.formatClock
import dev.nocturne.ambient.ui.theme.EyebrowType
import dev.nocturne.ambient.ui.theme.LabelType
import dev.nocturne.ambient.ui.theme.Palette
import dev.nocturne.ambient.ui.worldClock
import dev.nocturne.ambient.ui.worldDayDelta
import java.time.ZonedDateTime

@Composable
fun Hairline(palette: Palette, modifier: Modifier = Modifier, alpha: Float = 1f) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.hairline.copy(alpha = alpha))
    )
}

@Composable
fun Eyebrow(text: String, palette: Palette, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = EyebrowType,
        color = palette.inkFaint,
        modifier = modifier
    )
}

@Composable
fun EventLine(
    event: CalendarEvent,
    settings: Settings,
    palette: Palette,
    modifier: Modifier = Modifier,
    showCalendarName: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(palette.accent)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = event.title,
            style = LabelType,
            color = palette.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (showCalendarName && !event.calendarName.isNullOrBlank()) {
            Text(
                text = event.calendarName,
                style = EyebrowType.copy(letterSpacing = 1.1.sp),
                color = palette.inkFaint,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
        Text(
            text = eventTime(event, settings.use24Hour),
            style = LabelType,
            color = palette.inkSoft
        )
    }
}

@Composable
fun WorldClocks(
    palette: Palette,
    settings: Settings,
    modifier: Modifier = Modifier
) {
    val clocks = listOf(
        "Europe/London" to "LONDON",
        "America/New_York" to "NEW YORK",
        "Asia/Tokyo" to "TOKYO"
    )
    val now = ZonedDateTime.now()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        clocks.forEachIndexed { index, (zone, label) ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(palette.hairline)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = worldClock(zone, settings.use24Hour, now),
                    style = LabelType.copy(fontSize = 18.sp, letterSpacing = 0.4.sp),
                    color = palette.ink
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        style = EyebrowType,
                        color = palette.inkFaint
                    )
                    val delta = worldDayDelta(zone, now)
                    if (delta.isNotEmpty()) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = delta,
                            style = EyebrowType.copy(letterSpacing = 0.4.sp),
                            color = palette.accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SunTimes(
    sunrise: String,
    sunset: String,
    palette: Palette,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.End) {
            Eyebrow("Sunrise", palette)
            Text(
                text = sunrise,
                style = LabelType.copy(fontSize = 17.sp),
                color = palette.ink
            )
        }
        Spacer(Modifier.width(26.dp))
        Column {
            Eyebrow("Sunset", palette)
            Text(
                text = sunset,
                style = LabelType.copy(fontSize = 17.sp),
                color = palette.ink
            )
        }
    }
}

@Composable
fun NextEventLine(
    event: CalendarEvent?,
    palette: Palette,
    use24Hour: Boolean,
    modifier: Modifier = Modifier
) {
    if (event == null) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Eyebrow("Next", palette)
        Spacer(Modifier.width(12.dp))
        Text(
            text = event.title,
            style = LabelType,
            color = palette.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = eventTime(event, use24Hour),
            style = LabelType,
            color = palette.accent
        )
    }
}
