package dev.telepromptor.ui.settings

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import dev.telepromptor.BuildConfig

private const val LATEST_RELEASE = "https://github.com/rickevans86/Teleprompter/releases/latest"

@Composable
fun SettingsScreen(back: () -> Unit) {
    val browser = LocalUriHandler.current
    var error by remember { mutableStateOf(false) }

    fun open(url: String) {
        error =
            try {
                browser.openUri(url)
                false
            } catch (_: ActivityNotFoundException) {
                true
            } catch (_: IllegalArgumentException) {
                true
            }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(back) { Text("Back") }
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Installed version ${BuildConfig.VERSION_NAME}")
        Text("Download the latest release in your browser, then open the APK to install it. GitHub may ask you to sign in.")
        Button(onClick = { open("$LATEST_RELEASE/download/Telepromptor.apk") }) { Text("Update app") }
        TextButton(onClick = { open(LATEST_RELEASE) }) { Text("View release notes") }
        if (error) Text("A browser is required to open GitHub.", color = MaterialTheme.colorScheme.error)
    }
}
