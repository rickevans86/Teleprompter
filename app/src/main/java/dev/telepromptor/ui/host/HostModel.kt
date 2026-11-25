package dev.telepromptor.ui.host

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.telepromptor.bluetooth.LiveSession
import dev.telepromptor.data.SavedScript
import dev.telepromptor.protocol.Role
import dev.telepromptor.protocol.Script
import javax.inject.Inject

@HiltViewModel
class HostModel
    @Inject
    constructor(
        val live: LiveSession,
    ) : ViewModel() {
        val session = live.session

        fun startScript(script: SavedScript) {
            session.begin(Role.HOST)
            session.script(Script(script.id, script.title, script.text))
        }

        fun setPlaybackRate(textHeightPx: Int) {
            session.update {
                it.copy(fractionsPerSecond = (it.speedPxPerSec / textHeightPx.toDouble()).coerceAtMost(100.0))
            }
        }

        fun restart(countdown: Boolean) {
            session.stopCountdown()
            session.update { it.copy(playing = false) }
            session.seek(0.0)
            session.play(if (countdown) 3 else 0)
        }
    }
