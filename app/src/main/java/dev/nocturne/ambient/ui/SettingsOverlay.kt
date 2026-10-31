package dev.nocturne.ambient.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nocturne.ambient.ClockViewModel
import dev.nocturne.ambient.model.Settings
import dev.nocturne.ambient.model.WeatherData
import dev.nocturne.ambient.model.cityPresets
import dev.nocturne.ambient.model.zonePresets
import dev.nocturne.ambient.ui.components.Eyebrow
import dev.nocturne.ambient.ui.components.Hairline
import dev.nocturne.ambient.ui.components.MoonGlyph
import dev.nocturne.ambient.ui.components.WeatherGlyph
import dev.nocturne.ambient.ui.components.rememberNow
import dev.nocturne.ambient.ui.theme.EyebrowType
import dev.nocturne.ambient.ui.theme.LabelType
import dev.nocturne.ambient.ui.theme.Palette
import dev.nocturne.ambient.ui.theme.Sora
import dev.nocturne.ambient.ui.timeParts
import dev.nocturne.ambient.ui.temperature
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class SettingsPage { MAIN, LOCATION, CALENDAR, WORLD_CLOCK }

@Composable
fun SettingsOverlay(
    viewModel: ClockViewModel,
    settings: Settings,
    palette: Palette,
    onClose: () -> Unit
) {
    var page by remember { mutableStateOf(SettingsPage.MAIN) }
    var calendarDraft by remember(settings.calendarUrls) { mutableStateOf(settings.calendarUrls) }
    val focusRequester = remember { FocusRequester() }
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()

    BackHandler {
        if (page == SettingsPage.MAIN) onClose() else page = SettingsPage.MAIN
    }

    LaunchedEffect(page) {
        delay(90)
        runCatching { focusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 44.dp, vertical = 30.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.backgroundDeep.copy(alpha = 0.97f))
                .border(1.dp, palette.hairline, RoundedCornerShape(18.dp))
                .padding(horizontal = 28.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = when (page) {
                            SettingsPage.MAIN -> "Settings"
                            SettingsPage.LOCATION -> "Weather location"
                            SettingsPage.CALENDAR -> "Calendar feeds"
                            SettingsPage.WORLD_CLOCK -> "World clocks"
                        },
                        fontFamily = Sora,
                        fontWeight = FontWeight.Light,
                        fontSize = 29.sp,
                        lineHeight = 32.sp,
                        color = palette.ink
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = when (page) {
                            SettingsPage.MAIN -> "Nocturne 1.0  ·  Fire TV ambient display"
                            SettingsPage.LOCATION -> "Used for weather, sunrise and sunset"
                            SettingsPage.CALENDAR -> "Private iCal feeds, no account or OAuth"
                            SettingsPage.WORLD_CLOCK -> "Three cities shown on the Analog and Dashboard faces"
                        },
                        style = EyebrowType.copy(letterSpacing = 1.6.sp),
                        color = palette.inkFaint
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "BACK TO CLOSE",
                    style = EyebrowType.copy(letterSpacing = 1.8.sp),
                    color = palette.inkFaint
                )
            }

            Spacer(Modifier.height(16.dp))
            Hairline(palette)
            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (page) {
                        SettingsPage.MAIN -> MainSettings(
                            viewModel = viewModel,
                            settings = settings,
                            palette = palette,
                            syncing = syncing,
                            firstFocus = focusRequester,
                            onOpenLocation = { page = SettingsPage.LOCATION },
                            onOpenCalendar = { page = SettingsPage.CALENDAR },
                            onOpenWorldClocks = { page = SettingsPage.WORLD_CLOCK }
                        )
                        SettingsPage.LOCATION -> LocationSettings(
                            viewModel = viewModel,
                            settings = settings,
                            palette = palette,
                            firstFocus = focusRequester,
                            onDone = { page = SettingsPage.MAIN }
                        )
                        SettingsPage.CALENDAR -> CalendarSettings(
                            viewModel = viewModel,
                            palette = palette,
                            draft = calendarDraft,
                            onDraftChange = { calendarDraft = it },
                            focusRequester = focusRequester,
                            onDone = { page = SettingsPage.MAIN }
                        )
                        SettingsPage.WORLD_CLOCK -> WorldClockSettings(
                            viewModel = viewModel,
                            settings = settings,
                            palette = palette,
                            firstFocus = focusRequester,
                            onDone = { page = SettingsPage.MAIN }
                        )
                    }
                }

                Spacer(Modifier.width(26.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(palette.hairline)
                )
                Spacer(Modifier.width(24.dp))

                SettingsSidebar(
                    settings = settings,
                    weather = weather,
                    palette = palette,
                    modifier = Modifier.width(226.dp).fillMaxHeight()
                )
            }

            Spacer(Modifier.height(10.dp))
            Hairline(palette, alpha = 0.7f)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "← →  change face     ·     ↑ ↓  brightness     ·     OK  settings",
                    style = EyebrowType.copy(letterSpacing = 0.9.sp),
                    color = palette.inkFaint
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "WEATHER  ·  OPEN-METEO",
                    style = EyebrowType.copy(letterSpacing = 1.2.sp),
                    color = palette.inkFaint.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun MainSettings(
    viewModel: ClockViewModel,
    settings: Settings,
    palette: Palette,
    syncing: Boolean,
    firstFocus: FocusRequester,
    onOpenLocation: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenWorldClocks: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            SettingRow(
                label = "Face",
                value = settings.face.title,
                palette = palette,
                onClick = { viewModel.cycleFace(1) },
                onLeft = { viewModel.cycleFace(-1) },
                onRight = { viewModel.cycleFace(1) },
                modifier = Modifier.focusRequester(firstFocus)
            )
        }
        item {
            SettingRow(
                label = "Clock format",
                value = if (settings.use24Hour) "24-hour" else "12-hour",
                palette = palette,
                onClick = { viewModel.updateSettings { it.copy(use24Hour = !it.use24Hour) } }
            )
        }
        item {
            SettingRow(
                label = "Seconds",
                value = if (settings.showSeconds) "Shown" else "Hidden",
                palette = palette,
                valueAccent = settings.showSeconds,
                onClick = { viewModel.updateSettings { it.copy(showSeconds = !it.showSeconds) } }
            )
        }
        item {
            SettingRow(
                label = "Temperature",
                value = if (settings.celsius) "Celsius" else "Fahrenheit",
                palette = palette,
                onClick = { viewModel.updateSettings { it.copy(celsius = !it.celsius) } }
            )
        }
        item {
            SettingRow(
                label = "Brightness",
                value = "${(settings.brightness * 100).roundToInt()}%",
                palette = palette,
                onClick = {
                    val next = if (settings.brightness >= 0.99f) 0.2f else settings.brightness + 0.1f
                    viewModel.setBrightness(next)
                },
                onLeft = { viewModel.adjustBrightness(-0.05f) },
                onRight = { viewModel.adjustBrightness(0.05f) }
            )
        }
        item {
            SettingRow(
                label = "Night dimming",
                value = if (settings.nightDim) "On" else "Off",
                palette = palette,
                valueAccent = settings.nightDim,
                onClick = { viewModel.updateSettings { it.copy(nightDim = !it.nightDim) } }
            )
        }
        item {
            SettingRow(
                label = "Weather location",
                value = settings.locationLabel,
                palette = palette,
                onClick = onOpenLocation
            )
        }
        item {
            SettingRow(
                label = "Calendar feeds",
                value = if (settings.calendarUrls.isBlank()) "Not set"
                else "${settings.calendarUrls.lineSequence().count { it.isNotBlank() }} connected",
                palette = palette,
                valueAccent = settings.calendarUrls.isNotBlank(),
                onClick = onOpenCalendar
            )
        }
        item {
            SettingRow(
                label = "World clocks",
                value = if (!settings.showWorldClocks) "Off"
                else settings.worldClockCities.joinToString(" · ") { it.label },
                palette = palette,
                valueAccent = settings.showWorldClocks,
                onClick = onOpenWorldClocks
            )
        }
        item {
            SettingRow(
                label = "Burn-in shift",
                value = if (settings.motion) "On" else "Off",
                palette = palette,
                valueAccent = settings.motion,
                onClick = { viewModel.updateSettings { it.copy(motion = !it.motion) } }
            )
        }
        item {
            SettingRow(
                label = "Launch on boot",
                value = if (settings.launchOnBoot) "On" else "Off",
                palette = palette,
                valueAccent = settings.launchOnBoot,
                onClick = { viewModel.updateSettings { it.copy(launchOnBoot = !it.launchOnBoot) } }
            )
        }
        item {
            SettingRow(
                label = "Refresh data",
                value = if (syncing) "Updating…" else "Update now",
                palette = palette,
                onClick = { viewModel.refresh() }
            )
        }
    }
}

@Composable
private fun LocationSettings(
    viewModel: ClockViewModel,
    settings: Settings,
    palette: Palette,
    firstFocus: FocusRequester,
    onDone: () -> Unit
) {
    val usingAuto = settings.locationAuto
    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            SettingRow(
                label = "Auto-detect",
                value = if (usingAuto) "Selected" else "Use my IP",
                palette = palette,
                valueAccent = usingAuto,
                onClick = {
                    viewModel.useAutoLocation()
                    onDone()
                },
                modifier = Modifier.focusRequester(firstFocus)
            )
        }
        items(cityPresets.size) { index ->
            val city = cityPresets[index]
            val selected = !usingAuto && settings.locationLabel == city.label &&
                kotlin.math.abs(settings.latitude - city.latitude) < 0.01 &&
                kotlin.math.abs(settings.longitude - city.longitude) < 0.01
            SettingRow(
                label = city.label,
                value = if (selected) "Selected" else city.region,
                palette = palette,
                valueAccent = selected,
                onClick = {
                    viewModel.applyCity(city)
                    onDone()
                }
            )
        }
    }
}

@Composable
private fun CalendarSettings(
    viewModel: ClockViewModel,
    palette: Palette,
    draft: String,
    onDraftChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onDone: () -> Unit
) {
    var fieldFocused by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Paste one or more secret iCal addresses, one per line. Google Calendar: Settings → " +
                "your calendar → Secret address in iCal format. Apple/Outlook also publish private iCal links.",
            style = LabelType.copy(fontSize = 12.sp, lineHeight = 18.sp),
            color = palette.inkSoft
        )
        Spacer(Modifier.height(12.dp))
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            textStyle = TextStyle(
                fontFamily = dev.nocturne.ambient.ui.theme.Manrope,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = palette.ink
            ),
            cursorBrush = SolidColor(palette.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.surface.copy(alpha = 0.62f))
                .border(
                    width = 1.dp,
                    color = if (fieldFocused) palette.accent else palette.hairline,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(12.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { fieldFocused = it.isFocused },
            decorationBox = { inner ->
                Box {
                    if (draft.isBlank()) {
                        Text(
                            text = "https://calendar.google.com/calendar/ical/…/basic.ics",
                            style = LabelType.copy(fontSize = 12.sp),
                            color = palette.inkFaint
                        )
                    }
                    inner()
                }
            }
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "The feed is fetched directly by the TV and cached on-device. Only https links are accepted.",
            style = EyebrowType.copy(letterSpacing = 0.8.sp),
            color = palette.inkFaint
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsButton(
                label = "Save feeds",
                palette = palette,
                primary = true,
                onClick = {
                    viewModel.updateSettings { it.copy(calendarUrls = draft.trim()) }
                    viewModel.refresh()
                    onDone()
                }
            )
            SettingsButton(
                label = "Cancel",
                palette = palette,
                onClick = onDone
            )
        }
    }
}

@Composable
private fun WorldClockSettings(
    viewModel: ClockViewModel,
    settings: Settings,
    palette: Palette,
    firstFocus: FocusRequester,
    onDone: () -> Unit
) {
    fun cycle(slot: Int, direction: Int) {
        viewModel.updateSettings { s ->
            val cities = s.worldClockCities.toMutableList()
            while (cities.size < 3) cities.add(zonePresets[cities.size % zonePresets.size])
            val current = cities[slot]
            val index = zonePresets.indexOfFirst { it.zone == current.zone && it.label == current.label }
                .let { if (it < 0) 0 else it }
            cities[slot] = zonePresets[(index + direction + zonePresets.size) % zonePresets.size]
            s.copy(worldClockCities = cities)
        }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item {
            SettingRow(
                label = "Show world clocks",
                value = if (settings.showWorldClocks) "On" else "Off",
                palette = palette,
                valueAccent = settings.showWorldClocks,
                onClick = { viewModel.updateSettings { it.copy(showWorldClocks = !it.showWorldClocks) } },
                modifier = Modifier.focusRequester(firstFocus)
            )
        }
        items(3) { index ->
            val current = settings.worldClockCities.getOrElse(index) { zonePresets[0] }
            SettingRow(
                label = "City ${index + 1}",
                value = current.label,
                palette = palette,
                onClick = { cycle(index, 1) },
                onLeft = { cycle(index, -1) },
                onRight = { cycle(index, 1) }
            )
        }
        item {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Left/right cycles through cities. All three appear on the Analog and Dashboard faces.",
                style = EyebrowType.copy(letterSpacing = 0.8.sp),
                color = palette.inkFaint
            )
        }
        item {
            Spacer(Modifier.height(12.dp))
            SettingsButton(
                label = "Done",
                palette = palette,
                primary = true,
                onClick = onDone
            )
        }
    }
}

@Composable
private fun SettingsSidebar(
    settings: Settings,
    weather: WeatherData?,
    palette: Palette,
    modifier: Modifier = Modifier
) {
    val now = rememberNow(true)
    val parts = timeParts(now.toLocalTime(), settings.use24Hour)
    val moonPhase = dev.nocturne.ambient.ui.moonPhaseFraction(now.toLocalDate())

    Column(modifier = modifier) {
        Eyebrow("Live preview", palette)
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (settings.showSeconds) {
                "${parts.hour}:${parts.minute}:${parts.second}"
            } else {
                "${parts.hour}:${parts.minute}"
            },
            fontFamily = Sora,
            fontWeight = FontWeight.ExtraLight,
            fontSize = 46.sp,
            lineHeight = 48.sp,
            letterSpacing = (-1.5).sp,
            color = palette.ink
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = formattedDateShort(now),
            style = LabelType.copy(fontSize = 12.sp),
            color = palette.inkSoft
        )
        Spacer(Modifier.height(13.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (weather != null) {
                WeatherGlyph(
                    kind = weather.kind,
                    isDay = weather.isDay,
                    tint = palette.inkSoft,
                    accent = palette.accent,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${temperature(weather.temperature, settings.celsius)}  ${weather.description}",
                    style = LabelType.copy(fontSize = 11.sp),
                    color = palette.inkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text("Weather loading…", style = EyebrowType, color = palette.inkFaint)
            }
        }
        Spacer(Modifier.height(12.dp))
        Hairline(palette, alpha = 0.7f)
        Spacer(Modifier.height(12.dp))
        Eyebrow("Display", palette)
        Spacer(Modifier.height(6.dp))
        Text(settings.face.title, style = LabelType.copy(fontSize = 12.sp), color = palette.inkSoft)
        Spacer(Modifier.height(3.dp))
        Text(
            text = if (settings.use24Hour) "24-hour clock" else "12-hour clock",
            style = EyebrowType.copy(letterSpacing = 0.7.sp),
            color = palette.inkFaint
        )
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoonGlyph(
                phase = moonPhase,
                tint = palette.inkSoft,
                background = palette.backgroundDeep,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${dev.nocturne.ambient.ui.moonIllumination(moonPhase)}% illuminated",
                style = EyebrowType.copy(letterSpacing = 0.7.sp),
                color = palette.inkFaint
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "No accounts. No ads. No tracking.",
            style = EyebrowType.copy(letterSpacing = 0.7.sp),
            color = palette.inkFaint.copy(alpha = 0.62f)
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    palette: Palette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    valueAccent: Boolean = false,
    onLeft: (() -> Unit)? = null,
    onRight: (() -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.DirectionLeft && onLeft != null -> {
                        onLeft()
                        true
                    }
                    event.key == Key.DirectionRight && onRight != null -> {
                        onRight()
                        true
                    }
                    else -> false
                }
            }
            .onFocusChanged { focused = it.isFocused }
            .background(if (focused) palette.accent.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (focused) palette.accent.copy(alpha = 0.7f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = LabelType.copy(fontSize = 13.sp),
            color = if (focused) palette.ink else palette.inkSoft,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (onLeft != null || onRight != null) {
            Text(
                text = "‹",
                style = LabelType.copy(fontSize = 18.sp),
                color = palette.inkFaint,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Text(
                text = value,
                style = LabelType.copy(fontSize = 13.sp),
                color = if (valueAccent) palette.accent else palette.ink,
                maxLines = 1
            )
            Text(
                text = "›",
                style = LabelType.copy(fontSize = 18.sp),
                color = palette.inkFaint,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        } else {
            Text(
                text = value,
                style = LabelType.copy(fontSize = 13.sp),
                color = if (valueAccent) palette.accent else palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsButton(
    label: String,
    palette: Palette,
    onClick: () -> Unit,
    primary: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    primary && focused -> palette.accent
                    primary -> palette.accent.copy(alpha = 0.18f)
                    focused -> palette.accent.copy(alpha = 0.13f)
                    else -> palette.surface.copy(alpha = 0.4f)
                }
            )
            .border(
                1.dp,
                if (focused) palette.accent else palette.hairline,
                RoundedCornerShape(8.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = EyebrowType.copy(letterSpacing = 1.2.sp),
            color = if (primary && focused) palette.backgroundDeep else palette.ink
        )
    }
}

private fun formattedDateShort(now: LocalDateTime): String {
    return now.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM", java.util.Locale.getDefault()))
}
