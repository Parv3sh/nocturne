package dev.nocturne.ambient

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.nocturne.ambient.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            return
        }
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).current()
                if (settings.launchOnBoot) {
                    val launch = Intent(context, MainActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                    }
                    try {
                        context.startActivity(launch)
                    } catch (_: Exception) {
                        // Fire OS may decline background launches; the app is still one click away.
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
