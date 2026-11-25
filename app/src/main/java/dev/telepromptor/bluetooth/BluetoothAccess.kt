package dev.telepromptor.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

fun bluetoothPermissions(host: Boolean): Array<String> =
    if (Build.VERSION.SDK_INT >= 31) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            if (host) Manifest.permission.BLUETOOTH_ADVERTISE else Manifest.permission.BLUETOOTH_SCAN,
        )
    } else if (!host) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        emptyArray()
    }

@SuppressLint("MissingPermission")
@Composable
fun BluetoothAccess(
    adapter: BluetoothAdapter?,
    host: Boolean,
    back: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val permissions = remember(host) { bluetoothPermissions(host) }
    fun granted() = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    var allowed by remember { mutableStateOf(granted()) }
    var enabled by remember { mutableStateOf(adapter?.isEnabled == true) }
    var denied by remember { mutableStateOf(false) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        allowed = granted()
        denied = !allowed
    }
    val enable = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        enabled = adapter?.isEnabled == true
    }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        allowed = granted()
        enabled = adapter?.isEnabled == true
    }
    DisposableEffect(context, adapter) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                enabled = adapter?.isEnabled == true
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    if (adapter != null && allowed && enabled) {
        content()
        return
    }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(back) { Text("Back") }
        Text("Bluetooth setup", style = MaterialTheme.typography.headlineMedium)
        when {
            adapter == null -> Text("This device does not support Bluetooth Classic.")
            !allowed -> {
                Text(
                    if (host) "Allow Nearby devices so clients can find and connect to this host."
                    else if (Build.VERSION.SDK_INT >= 31) "Allow Nearby devices to find and connect to a host."
                    else "Android requires Location permission for Bluetooth discovery on this version. Telepromptor does not request your coordinates.",
                )
                if (denied) Text("Permission was denied. You can grant access in Android Settings.")
                Button({ request.launch(permissions) }) { Text("Allow access") }
                if (denied) OutlinedButton({
                    settings.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)))
                }) { Text("Open settings") }
            }
            else -> {
                Text("Turn on Bluetooth to use this mode.")
                Button({ enable.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)) }) { Text("Enable Bluetooth") }
            }
        }
    }
}
