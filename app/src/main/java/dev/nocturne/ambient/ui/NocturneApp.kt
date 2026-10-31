package dev.nocturne.ambient.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nocturne.ambient.ClockViewModel
import dev.nocturne.ambient.model.Face
import dev.nocturne.ambient.ui.components.NocturneBackdrop
import dev.nocturne.ambient.ui.components.rememberNow
import dev.nocturne.ambient.ui.faces.AmbientFace
import dev.nocturne.ambient.ui.faces.AnalogFace
import dev.nocturne.ambient.ui.faces.DashboardFace
import dev.nocturne.ambient.ui.faces.FlipFace
import dev.nocturne.ambient.ui.faces.MinimalFace
import dev.nocturne.ambient.ui.theme.EyebrowType
import dev.nocturne.ambient.ui.theme.LabelType
import dev.nocturne.ambient.ui.theme.Palette
import dev.nocturne.ambient.ui.theme.paletteFor
import kotlinx.coroutines.delay

@Composable
fun NocturneApp(
    viewModel: ClockViewModel,
    interactive: Boolean = true
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val showSettings by viewModel.showSettings.collectAsStateWithLifecycle()
    val hud by viewModel.hud.collectAsStateWithLifecycle()

    val highFrequency = settings.showSeconds ||
        settings.face == Face.ANALOG ||
        settings.face == Face.FLIP
    val now = rememberNow(highFrequency)
    val palette = paletteFor(now.hour, now.minute)

    var hintVisible by remember { mutableStateOf(interactive && !settings.onboardingSeen) }
    LaunchedEffect(interactive) {
        if (interactive && !settings.onboardingSeen) {
            delay(9_000)
            hintVisible = false
            viewModel.updateSettings { it.copy(onboardingSeen = true) }
        }
    }

    val density = LocalDensity.current
    val minutes = now.hour * 60L + now.minute
    val shiftX = if (settings.motion) smoothShift(minutes, 2.4f, true) else 0f
    val shiftY = if (settings.motion) smoothShift(minutes + 347, 1.4f, true) else 0f

    val dim = dimLevel(now, settings)

    Box(modifier = Modifier.fillMaxSize().background(palette.backgroundDeep)) {
        NocturneBackdrop(
            palette = palette,
            ambient = settings.face == Face.AMBIENT,
            motion = settings.motion
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = with(density) { shiftX.dp.toPx() }
                    translationY = with(density) { shiftY.dp.toPx() }
                }
        ) {
            Crossfade(
                targetState = settings.face,
                animationSpec = tween(if (settings.motion) 420 else 0),
                label = "face"
            ) { face ->
                when (face) {
                    Face.MINIMAL -> MinimalFace(settings, weather, events, now, palette, note = note)
                    Face.FLIP -> FlipFace(settings, weather, events, now, palette)
                    Face.ANALOG -> AnalogFace(settings, weather, events, now, palette)
                    Face.DASHBOARD -> DashboardFace(settings, weather, events, now, palette)
                    Face.AMBIENT -> AmbientFace(settings, weather, now, palette)
                }
            }
        }

        if (dim > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dim))
            )
        }

        if (interactive) {
            if (showSettings) {
                SettingsOverlay(
                    viewModel = viewModel,
                    settings = settings,
                    palette = palette,
                    onClose = { viewModel.setShowSettings(false) }
                )
            } else {
                val hint = buildString {
                    append("← →  face")
                    append("     ·     ↑ ↓  brightness")
                    append("     ·     OK  settings")
                }
                AnimatedVisibility(
                    visible = hud != null || hintVisible,
                    enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 },
                    exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { it / 2 },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 22.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(palette.surface.copy(alpha = 0.86f))
                            .border(1.dp, palette.hairline.copy(alpha = 0.8f), RoundedCornerShape(50))
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hud != null) {
                            Text(
                                text = hud!!,
                                style = LabelType.copy(fontSize = 12.sp),
                                color = palette.ink
                            )
                        } else {
                            Text(
                                text = hint,
                                style = EyebrowType.copy(letterSpacing = 1.4.sp),
                                color = palette.inkSoft
                            )
                        }
                    }
                }
            }
        }
    }
}
