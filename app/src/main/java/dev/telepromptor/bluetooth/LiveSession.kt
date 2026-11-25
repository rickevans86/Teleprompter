package dev.telepromptor.bluetooth

import android.annotation.SuppressLint
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import dev.telepromptor.protocol.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveSession
    @Inject
    constructor(
        val radio: RfcommTransport,
    ) {
        var volumeShortcut = VolumeShortcut.OFF
        var volumeCountdown = false

        fun togglePlayback(countdown: Boolean) {
            val view = session.view.value
            if (view.script == null) return
            when {
                view.countdownUntilMs != 0L -> session.stopCountdown()
                view.motion.state.playing -> session.update { it.copy(playing = false) }
                else -> session.play(if (countdown) 3 else 0)
            }
        }

        fun handleVolumeKey(event: KeyEvent): Boolean {
            if (volumeShortcut == VolumeShortcut.OFF ||
                event.keyCode !in setOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN)
            ) {
                return false
            }
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (volumeShortcut) {
                    VolumeShortcut.OFF -> Unit
                    VolumeShortcut.PLAY_PAUSE -> if (event.repeatCount == 0) togglePlayback(volumeCountdown)
                    VolumeShortcut.SPEED -> {
                        val amount = if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) 5 else -5
                        session.update { it.copy(speedPxPerSec = (it.speedPxPerSec + amount).coerceIn(10f, 400f)) }
                    }
                }
            }
            return true
        }

        @SuppressLint("MissingPermission")
        val session =
            Session(
                radio,
                CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                SystemClock::elapsedRealtime,
            ) { runCatching { radio.adapter?.name }.getOrNull() ?: Build.MODEL }
    }

enum class VolumeShortcut(
    val label: String,
) {
    OFF("Off"),
    SPEED("Speed"),
    PLAY_PAUSE("Play/pause"),
}
