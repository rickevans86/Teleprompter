package dev.telepromptor.ui.host

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.telepromptor.audio.microphoneActivity
import dev.telepromptor.bluetooth.LiveSession
import dev.telepromptor.data.SavedScript
import dev.telepromptor.protocol.Role
import dev.telepromptor.protocol.Script
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HostModel
    @Inject
    constructor(
        val live: LiveSession,
    ) : ViewModel() {
        val session = live.session
        private val mutableVoiceStatus = MutableStateFlow(VoiceStatus.OFF)
        val voiceStatus = mutableVoiceStatus.asStateFlow()
        private var microphone: Job? = null

        init {
            viewModelScope.launch {
                session.view.collect { view ->
                    if (view.script == null || (!view.motion.state.playing && view.motion.state.scrollOffset >= 1)) {
                        stopVoice()
                    }
                }
            }
        }

        fun startVoice() {
            val view = session.view.value
            if (microphone?.isActive == true || view.script == null) return
            session.stopCountdown()
            session.update { it.copy(playing = false) }
            if (session.view.value.motion.state.scrollOffset >= 1) session.seek(0.0)
            mutableVoiceStatus.value = VoiceStatus.STARTING
            val previous = microphone
            microphone =
                viewModelScope.launch {
                    previous?.join()
                    microphoneActivity()
                        .catch {
                            session.update { it.copy(playing = false) }
                            mutableVoiceStatus.value = VoiceStatus.ERROR
                        }.collect { speaking ->
                            session.update { it.copy(playing = speaking && it.scrollOffset < 1) }
                            mutableVoiceStatus.value = if (speaking) VoiceStatus.SPEAKING else VoiceStatus.LISTENING
                        }
                }
        }

        fun stopVoice() {
            microphone?.cancel()
            val wasActive = voiceStatus.value.active
            mutableVoiceStatus.value = VoiceStatus.OFF
            if (wasActive) session.update { it.copy(playing = false) }
        }

        fun togglePlayback(countdown: Boolean) {
            if (voiceStatus.value.active) stopVoice() else live.togglePlayback(countdown)
        }

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
            stopVoice()
            session.stopCountdown()
            session.update { it.copy(playing = false) }
            session.seek(0.0)
            session.play(if (countdown) 3 else 0)
        }
    }

enum class VoiceStatus(
    val active: Boolean,
) {
    OFF(false),
    STARTING(true),
    LISTENING(true),
    SPEAKING(true),
    ERROR(false),
}
