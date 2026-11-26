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
import dev.telepromptor.update.LATEST_RELEASE_PAGE
import dev.telepromptor.update.UpdateState

internal fun updateButtonLabel(state: UpdateState): String =
    when (state) {
        UpdateState.Checking -> "Checking…"
        is UpdateState.Available -> "Update ready — tap to install"
        is UpdateState.NeedPermission -> "Continue installing"
        UpdateState.Downloading -> "Downloading…"
        is UpdateState.Installing -> "Install again"
        UpdateState.Idle, UpdateState.UpToDate, is UpdateState.Failed -> "Check for updates"
    }

internal fun updateMessage(state: UpdateState): String? =
    when (state) {
        UpdateState.UpToDate -> "You have the latest version."
        is UpdateState.Available -> "Version ${state.tag} is available."
        is UpdateState.NeedPermission ->
            "Allow Telepromptor to install apps on the screen that opened, come back, then tap Continue installing."
        is UpdateState.Installing -> "Follow the Android installer prompts."
        is UpdateState.Failed -> "Update failed: ${state.reason}"
        UpdateState.Idle, UpdateState.Checking, UpdateState.Downloading -> null
    }

@Composable
fun SettingsScreen(
    back: () -> Unit,
    update: UpdateState,
    onUpdate: () -> Unit,
) {
    val browser = LocalUriHandler.current
    var browserMissing by remember { mutableStateOf(false) }
    val busy = update == UpdateState.Checking || update == UpdateState.Downloading

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
        Text("Telepromptor checks GitHub for a newer release, downloads it and opens the Android installer.")
        Button(onClick = onUpdate, enabled = !busy) { Text(updateButtonLabel(update)) }
        updateMessage(update)?.let {
            Text(it, color = if (update is UpdateState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
        TextButton(
            onClick = {
                browserMissing =
                    try {
                        browser.openUri(LATEST_RELEASE_PAGE)
                        false
                    } catch (_: ActivityNotFoundException) {
                        true
                    } catch (_: IllegalArgumentException) {
                        true
                    }
            },
        ) { Text("View release notes") }
        if (browserMissing) Text("A browser is required to open GitHub.", color = MaterialTheme.colorScheme.error)
    }
}
