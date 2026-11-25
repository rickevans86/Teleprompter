package dev.telepromptor.ui.client

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.telepromptor.bluetooth.BluetoothAccess
import dev.telepromptor.bluetooth.LiveSession
import dev.telepromptor.protocol.Role
import dev.telepromptor.protocol.SessionView
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun ClientRoute(live: LiveSession, back: () -> Unit) {
    val adapter = live.radio.adapter
    BluetoothAccess(adapter, host = false, back = back) {
        var address by remember { mutableStateOf<String?>(null) }
        var status by remember { mutableStateOf("Select the phone running HOST mode.") }
        var display by remember { mutableStateOf(false) }
        val view by live.session.view.collectAsStateWithLifecycle()
        DisposableEffect(live) {
            live.session.begin(Role.CLIENT)
            onDispose { live.session.endIf(Role.CLIENT) }
        }
        LaunchedEffect(address) {
            val target = address ?: return@LaunchedEffect
            status = "Connecting…"
            display = withTimeoutOrNull(20_000) { live.radio.connect(target) } == true
            if (!display) {
                status = "Could not connect. Check the host and try again."
                address = null
            }
        }
        val disconnect = {
            address = null
            display = false
            status = "Disconnected"
            live.session.endIf(Role.CLIENT)
            live.session.begin(Role.CLIENT)
        }
        if (display) {
            BackHandler(onBack = back)
            ClientDisplay(view, if (view.peers.isEmpty()) "Host connection unavailable" else null, disconnect, back)
        } else if (adapter != null) {
            DevicePicker(adapter, status, back) { address = it }
        }
    }
}

@Composable
fun ClientDisplay(view: SessionView, status: String?, disconnect: () -> Unit, back: () -> Unit) {
    val window = (LocalContext.current as? Activity)?.window
    DisposableEffect(window) {
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    Box(Modifier.fillMaxSize()) {
        PromptDisplay(view, Modifier.fillMaxSize())
        Row(Modifier.safeDrawingPadding().padding(horizontal = 8.dp)) {
            FilledTonalButton(back) { Text("Back") }
            Spacer(Modifier.weight(1f))
            FilledTonalButton(disconnect) { Text("Disconnect") }
        }
        status?.let {
            Surface(Modifier.align(androidx.compose.ui.Alignment.BottomCenter).safeDrawingPadding()) {
                Text(it, Modifier.padding(10.dp))
            }
        }
    }
}
