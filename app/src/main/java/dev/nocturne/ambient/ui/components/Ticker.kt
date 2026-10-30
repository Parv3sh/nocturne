package dev.nocturne.ambient.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.LocalDateTime

@Composable
fun rememberNow(highFrequency: Boolean): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(highFrequency) {
        while (true) {
            now = LocalDateTime.now()
            val delayMs = if (highFrequency) {
                1_000L - (now.nano / 1_000_000L)
            } else {
                60_000L - (now.second * 1_000L + now.nano / 1_000_000L)
            }
            delay(delayMs.coerceIn(80L, 60_000L))
        }
    }
    return now
}
