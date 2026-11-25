package dev.telepromptor.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.telepromptor.update.AvailableUpdate

@Composable
fun HomeScreen(
    host: () -> Unit,
    client: () -> Unit,
    settings: () -> Unit,
    update: AvailableUpdate? = null,
    download: () -> Unit = {},
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
        if (update != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Update available: version ${update.version}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(download) { Text("Download") }
                        TextButton(dismissUpdate) { Text("Later") }
                    }
                }
            }
        }
        Text("Telepromptor", style = MaterialTheme.typography.headlineLarge)
        Button(host, Modifier.fillMaxWidth()) { Text("HOST") }
        Text("Write a script and control playback")
        OutlinedButton(client, Modifier.fillMaxWidth()) { Text("CLIENT") }
        Text("Connect to a host and display the script")
        TextButton(settings) { Text("Settings") }
    }
}
