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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.telepromptor.bluetooth.BluetoothAccess
import dev.telepromptor.protocol.SessionView

@Composable
fun ClientRoute(
    back: () -> Unit,
    model: ClientModel = hiltViewModel(),
) {
    val adapter = model.live.radio.adapter
    val view by model.live.session.view
        .collectAsStateWithLifecycle()
    val connection by model.connection.collectAsStateWithLifecycle()
    val leave = {
        model.disconnect()
        back()
    }
    BackHandler(onBack = leave)
    if (connection.showDisplay) {
        ClientDisplay(view, connection.status, model::disconnect, leave)
    } else {
        BluetoothAccess(adapter, host = false, back = leave) {
            if (adapter != null) {
                DevicePicker(
                    adapter,
                    connection.status.orEmpty(),
                    leave,
                    model::connect,
                    connecting = connection.connecting,
                )
            }
        }
    }
}

@Composable
fun ClientDisplay(
    view: SessionView,
    status: String?,
    disconnect: () -> Unit,
    back: () -> Unit,
) {
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
