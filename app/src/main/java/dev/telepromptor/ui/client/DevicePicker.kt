package dev.telepromptor.ui.client

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@SuppressLint("MissingPermission")
@Composable
fun DevicePicker(adapter: BluetoothAdapter, status: String, back: () -> Unit, select: (String) -> Unit) {
    val context = LocalContext.current
    val found = remember { mutableStateMapOf<String, String>() }
    var scanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf(false) }
    DisposableEffect(adapter, context) {
        fun add(device: BluetoothDevice) {
            val name = runCatching { device.name }.getOrNull()
            found[device.address] = name ?: device.address
        }
        runCatching { adapter.bondedDevices.forEach(::add) }
        val receiver = object : BroadcastReceiver() {
            @Suppress("DEPRECATION")
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> scanning = false
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = if (Build.VERSION.SDK_INT >= 33) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        device?.let(::add)
                    }
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND).apply {
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        onDispose {
            context.unregisterReceiver(receiver)
            runCatching { adapter.cancelDiscovery() }
        }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) {
        TextButton(back) { Text("Back") }
        Text("Connect to a host", style = MaterialTheme.typography.headlineMedium)
        Text(status)
        Button(onClick = {
            scanning = runCatching { adapter.startDiscovery() }.getOrDefault(false)
            scanError = !scanning
        }, enabled = !scanning) { Text(if (scanning) "Scanning…" else "Scan nearby") }
        if (scanError) Text("Scan unavailable. Check Bluetooth and, on older Android versions, turn on Location.")
        if (found.isEmpty()) Text("Make the host discoverable, or pair both devices in Android Settings.")
        LazyColumn {
            items(found.entries.sortedBy { it.value }, key = { it.key }) { (address, name) ->
                TextButton(onClick = { select(address) }) {
                    Column {
                        Text(name)
                        if (name != address) Text(address, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
