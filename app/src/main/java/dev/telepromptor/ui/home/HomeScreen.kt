package dev.telepromptor.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.telepromptor.update.UpdateState

@Composable
fun HomeScreen(
    host: () -> Unit,
    client: () -> Unit,
    settings: () -> Unit,
    update: UpdateState = UpdateState.Idle,
    onUpdate: () -> Unit = {},
    dismissUpdate: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UpdateBanner(update, onUpdate, dismissUpdate)
        Text("Telepromptor", style = MaterialTheme.typography.headlineLarge)
        Button(host, Modifier.fillMaxWidth()) { Text("HOST") }
        Text("Write a script and control playback")
        OutlinedButton(client, Modifier.fillMaxWidth()) { Text("CLIENT") }
        Text("Connect to a host and display the script")
        TextButton(settings) { Text("Settings") }
    }
}

@Composable
private fun UpdateBanner(
    update: UpdateState,
    onUpdate: () -> Unit,
    dismiss: () -> Unit,
) {
    val message =
        when (update) {
            is UpdateState.Available -> "Update available: ${update.tag}"
            is UpdateState.NeedPermission -> "Allow installs on the screen that opened, then continue."
            UpdateState.Downloading -> "Downloading the update…"
            is UpdateState.Installing -> "Follow the Android installer prompts."
            else -> return
        }
    val action =
        when (update) {
            is UpdateState.Available -> "Install"
            is UpdateState.NeedPermission -> "Continue"
            is UpdateState.Installing -> "Install again"
            else -> null
        }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message)
            if (action != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onUpdate) { Text(action) }
                    TextButton(dismiss) { Text("Later") }
                }
            }
        }
    }
}
