package dev.nocturne.ambient

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dev.nocturne.ambient.model.Face
import dev.nocturne.ambient.ui.NocturneApp

class MainActivity : ComponentActivity() {
    private val viewModel: ClockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUi()
        setContent {
            NocturneApp(viewModel = viewModel, interactive = true)
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            viewModel.toggleSettings()
            return true
        }
        if (viewModel.showSettings.value) {
            return super.onKeyDown(keyCode, event)
        }
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                viewModel.cycleFace(-1)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                viewModel.cycleFace(1)
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                viewModel.adjustBrightness(0.05f)
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                viewModel.adjustBrightness(-0.05f)
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                viewModel.setShowSettings(true)
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (viewModel.armExit()) {
                    finish()
                }
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                viewModel.setFace(Face.AMBIENT)
                true
            }
            KeyEvent.KEYCODE_1 -> jumpToFace(0)
            KeyEvent.KEYCODE_2 -> jumpToFace(1)
            KeyEvent.KEYCODE_3 -> jumpToFace(2)
            KeyEvent.KEYCODE_4 -> jumpToFace(3)
            KeyEvent.KEYCODE_5 -> jumpToFace(4)
            KeyEvent.KEYCODE_INFO -> {
                val weather = viewModel.weather.value
                viewModel.showHud(
                    if (weather != null) {
                        "${weather.description} · ${weather.humidity}% humidity · ${weather.windKph.toInt()} km/h"
                    } else {
                        "No weather data yet"
                    },
                    3_000L
                )
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun jumpToFace(index: Int): Boolean {
        val faces = Face.entries
        if (index in faces.indices) viewModel.setFace(faces[index])
        return true
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }
}
