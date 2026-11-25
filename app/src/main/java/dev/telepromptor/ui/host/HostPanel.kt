package dev.telepromptor.ui.host

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.telepromptor.bluetooth.VolumeShortcut
import dev.telepromptor.protocol.Playback
import dev.telepromptor.protocol.SessionView
import dev.telepromptor.ui.client.PromptDisplay
import dev.telepromptor.ui.client.rememberFrameTime
import kotlin.math.roundToInt

@Composable
fun HostPanel(
    view: SessionView,
    countdown: Boolean,
    shortcut: VolumeShortcut,
    back: () -> Unit,
    connect: () -> Unit,
    playPause: () -> Unit,
    restart: () -> Unit,
    seek: (Double) -> Unit,
    update: ((Playback) -> Playback) -> Unit,
    onCountdown: (Boolean) -> Unit,
    onShortcut: (VolumeShortcut) -> Unit,
    onTravel: (Int) -> Unit,
) {
    val state = view.motion.state
    val enabled = view.script != null
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(back) { Text("Editor") }
            Spacer(Modifier.weight(1f))
            OutlinedButton(connect) { Text("Clients (${view.peers.size})") }
        }
        Text(view.script?.title ?: "No active script", style = MaterialTheme.typography.headlineSmall)
        PromptDisplay(view, Modifier.fillMaxWidth().height(160.dp), onTravel)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(playPause, enabled = enabled) {
                Text(when {
                    view.countdownUntilMs != 0L -> "Cancel countdown"
                    state.playing -> "Pause"
                    else -> "Play"
                })
            }
            OutlinedButton(restart, enabled = enabled) { Text("Restart") }
        }
        PositionControl(view, enabled, seek)
        Text("Scroll speed · ${state.speedPxPerSec.roundToInt()} px/s")
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton({ update { it.copy(speedPxPerSec = (it.speedPxPerSec - 5).coerceAtLeast(10f)) } }) { Text("−") }
            Slider(
                state.speedPxPerSec, { value -> update { it.copy(speedPxPerSec = value) } },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).testTag("scroll-speed"),
                valueRange = 10f..400f,
            )
            OutlinedButton({ update { it.copy(speedPxPerSec = (it.speedPxPerSec + 5).coerceAtMost(400f)) } }) { Text("+") }
        }
        Text("Font size · ${state.fontSizeSp.roundToInt()} sp")
        Slider(state.fontSizeSp, { value -> update { it.copy(fontSizeSp = value) } }, valueRange = 16f..100f)
        Setting("Mirror horizontally", state.mirror) { value -> update { it.copy(mirror = value) } }
        Setting("Dark display", state.theme == "dark") { value -> update { it.copy(theme = if (value) "dark" else "light") } }
        Setting("Center text", state.align == "center") { value -> update { it.copy(align = if (value) "center" else "left") } }
        Setting("3-second countdown", countdown, onCountdown)
        Text("Volume keys")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VolumeShortcut.entries.forEach { item ->
                FilterChip(
                    selected = shortcut == item,
                    onClick = { onShortcut(item) },
                    label = { Text(item.label) },
                )
            }
        }
    }
}

@Composable
private fun PositionControl(view: SessionView, enabled: Boolean, seek: (Double) -> Unit) {
    val frame = rememberFrameTime(view.motion.state.playing, view.motion)
    var drag by remember { mutableStateOf<Float?>(null) }
    val position = drag ?: view.motion.position(frame.value).toFloat()
    Text("Position · ${(position * 100).roundToInt()}%")
    Slider(
        position,
        onValueChange = { drag = it },
        modifier = Modifier.testTag("script-position"),
        enabled = enabled,
        onValueChangeFinished = {
            drag?.let { seek(it.toDouble()) }
            drag = null
        },
    )
}

@Composable
private fun Setting(label: String, checked: Boolean, changed: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = changed)
    }
}
