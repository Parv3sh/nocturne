package dev.nocturne.ambient.ui.faces

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nocturne.ambient.model.Settings
import dev.nocturne.ambient.model.WeatherData
import dev.nocturne.ambient.ui.formattedDate
import dev.nocturne.ambient.ui.moonIllumination
import dev.nocturne.ambient.ui.moonPhaseFraction
import dev.nocturne.ambient.ui.temperature
import dev.nocturne.ambient.ui.theme.ClockSuffixType
import dev.nocturne.ambient.ui.theme.EyebrowType
import dev.nocturne.ambient.ui.theme.LabelType
import dev.nocturne.ambient.ui.theme.Palette
import dev.nocturne.ambient.ui.theme.Sora
import dev.nocturne.ambient.ui.timeParts
import dev.nocturne.ambient.ui.components.MoonGlyph
import dev.nocturne.ambient.ui.components.WeatherGlyph
import java.time.LocalDateTime

@Composable
fun AmbientFace(
    settings: Settings,
    weather: WeatherData?,
    now: LocalDateTime,
    palette: Palette,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val timeSize = if (maxWidth > 760.dp) 126.sp else 92.sp
        val parts = timeParts(now.toLocalTime(), settings.use24Hour)
        val moonPhase = moonPhaseFraction(now.toLocalDate())

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formattedDate(now.toLocalDate()).uppercase(),
                    style = EyebrowType.copy(letterSpacing = 3.4.sp),
                    color = palette.inkFaint
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${parts.hour}:${parts.minute}",
                        fontFamily = Sora,
                        fontWeight = FontWeight.ExtraLight,
                        fontSize = timeSize,
                        lineHeight = timeSize,
                        letterSpacing = (-4).sp,
                        color = palette.ink
                    )
                    if (parts.suffix.isNotEmpty()) {
                        Text(
                            text = parts.suffix,
                            style = ClockSuffixType,
                            color = palette.inkFaint,
                            modifier = Modifier.padding(start = 12.dp, bottom = 20.dp)
                        )
                    }
                }
                if (settings.showWeather && weather != null) {
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WeatherGlyph(
                            kind = weather.kind,
                            isDay = weather.isDay,
                            tint = palette.inkSoft,
                            accent = palette.accent,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${temperature(weather.temperature, settings.celsius)}  ·  ${weather.description}",
                            style = LabelType.copy(fontSize = 14.sp),
                            color = palette.inkSoft
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MoonGlyph(
                    phase = moonPhase,
                    tint = palette.inkSoft,
                    background = palette.background,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "${moonIllumination(moonPhase)}%",
                    style = EyebrowType.copy(letterSpacing = 1.1.sp),
                    color = palette.inkFaint
                )
            }
        }
    }
}
