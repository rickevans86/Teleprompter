package dev.telepromptor.ui.host

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.telepromptor.bluetooth.BluetoothAccess
import dev.telepromptor.bluetooth.LiveSession
import dev.telepromptor.bluetooth.VolumeShortcut
import dev.telepromptor.protocol.Role
import dev.telepromptor.service.HostService

@Composable
fun HostRoute(
    back: () -> Unit,
    model: HostModel = hiltViewModel(),
) {
    val live = model.live
    val context = LocalContext.current
    val view by live.session.view.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf("editor") }
    var useCountdown by rememberSaveable { mutableStateOf(false) }
    var speedSpan by remember { mutableIntStateOf(1) }
    var shortcut by rememberSaveable { mutableStateOf(VolumeShortcut.OFF) }
    val session = model.session

    fun stopAndLeave() {
        context.stopService(Intent(context, HostService::class.java))
        session.endIf(Role.HOST)
        back()
    }

    BackHandler {
        when (section) {
            "connect" -> section = "panel"
            "panel" -> section = "editor"
            else -> stopAndLeave()
        }
    }
    DisposableEffect(live, section, shortcut, useCountdown) {
        live.volumeShortcut = if (section == "panel") shortcut else VolumeShortcut.OFF
        live.volumeCountdown = useCountdown
        onDispose { live.volumeShortcut = VolumeShortcut.OFF }
    }

    when (section) {
        "connect" ->
            BluetoothAccess(live.radio.adapter, host = true, back = { section = "panel" }) {
                HostConnections(live, back = { section = "panel" })
            }
        "panel" -> {
            LaunchedEffect(speedSpan, view.motion.state.speedPxPerSec) {
                model.setPlaybackRate(speedSpan)
            }
            HostPanel(
                view = view,
                countdown = useCountdown,
                shortcut = shortcut,
                back = { section = "editor" },
                connect = { section = "connect" },
                playPause = { live.togglePlayback(useCountdown) },
                restart = { model.restart(useCountdown) },
                seek = session::seek,
                update = session::update,
                onCountdown = { useCountdown = it },
                onShortcut = { shortcut = it },
                onTravel = { speedSpan = it },
            )
        }
        else ->
            ScriptEditor(back = ::stopAndLeave, live = {
                model.startScript(it)
                section = "panel"
            })
    }
}

@Composable
private fun HostConnections(
    live: LiveSession,
    back: () -> Unit,
) {
    val context = LocalContext.current
    val state by live.radio.listening.collectAsStateWithLifecycle()
    val view by live.session.view.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf("") }
    val discoverable =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            message = if (it.resultCode > 0) "Discoverable for ${it.resultCode} seconds" else "Discoverability was canceled"
        }
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(back) { Text("Back") }
        Text("Client connections", style = MaterialTheme.typography.headlineMedium)
        Text(state.ifBlank { "Hosting is off" })
        if (state.isBlank() || state.startsWith("Bluetooth listener stopped")) {
            Button(onClick = {
                val intent = Intent(context, HostService::class.java).setAction(HostService.RETRY)
                runCatching { ContextCompat.startForegroundService(context, intent) }
                    .onFailure { message = "Could not start hosting. Check Bluetooth permissions." }
            }) { Text("Start hosting") }
        }
        Button(
            onClick = {
                val intent =
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                        .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                runCatching { discoverable.launch(intent) }
                    .onFailure { message = "Could not make this device discoverable." }
            },
            enabled = state == "Waiting for clients",
        ) { Text("Make discoverable") }
        if (message.isNotBlank()) Text(message)
        Text("${view.peers.size} of 3 clients connected")
        view.peers.forEach { (id, name) ->
            Row {
                Text(name, Modifier.weight(1f).padding(top = 12.dp))
                TextButton({ live.session.disconnect(id) }) { Text("Disconnect") }
            }
        }
    }
}
