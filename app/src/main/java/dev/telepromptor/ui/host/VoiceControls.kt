package dev.telepromptor.ui.host

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun VoiceControls(
    model: HostModel,
    enabled: Boolean,
) {
    val context = LocalContext.current
    val status by model.voiceStatus.collectAsStateWithLifecycle()
    var denied by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf(false) }
    val permission = Manifest.permission.RECORD_AUDIO
    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            denied = !granted
            pendingStart = granted
        }
    LifecycleResumeEffect(model) {
        visible = true
        onPauseOrDispose {
            visible = false
            pendingStart = false
            model.stopVoice()
        }
    }
    LaunchedEffect(visible, pendingStart, enabled) {
        if (visible && pendingStart && enabled) {
            pendingStart = false
            model.startVoice()
        }
    }
    if (!status.active) {
        OutlinedButton(
            onClick = {
                denied = false
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    pendingStart = true
                } else {
                    request.launch(permission)
                }
            },
            enabled = enabled,
        ) { Text("Start voice scrolling") }
    }
    val message =
        when {
            denied -> "Microphone access is off. You can allow it in Android Settings."
            status == VoiceStatus.ERROR -> "Microphone unavailable. Try again or use Play."
            status == VoiceStatus.STARTING -> "Starting microphone…"
            status == VoiceStatus.LISTENING -> "Listening for speech…"
            status == VoiceStatus.SPEAKING -> "Scrolling while you speak"
            else -> "Speak to scroll at the selected speed. Audio stays on this device."
        }
    Text(message, style = MaterialTheme.typography.bodySmall)
}
