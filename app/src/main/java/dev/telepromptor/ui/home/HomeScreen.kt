package dev.telepromptor.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    host: () -> Unit,
    client: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Telepromptor", style = MaterialTheme.typography.headlineLarge)
        Button(host, Modifier.fillMaxWidth()) { Text("HOST") }
        Text("Write a script and control playback")
        OutlinedButton(client, Modifier.fillMaxWidth()) { Text("CLIENT") }
        Text("Connect to a host and display the script")
    }
}
