package dev.telepromptor.ui.host

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.telepromptor.data.SavedScript
import dev.telepromptor.data.Scripts
import dev.telepromptor.protocol.MAX_SCRIPT_BYTES
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@HiltViewModel
class ScriptEditorModel @Inject constructor(val scripts: Scripts) : ViewModel()

@Composable
fun ScriptEditor(
    back: () -> Unit,
    live: ((SavedScript) -> Unit)? = null,
    model: ScriptEditorModel = viewModel(),
) {
    val saved by model.scripts.observe().collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var id by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<SavedScript?>(null) }
    val draft = SavedScript(id, title.trim().ifBlank { "Untitled script" }, body)
    val valid = title.length <= 200 && body.isNotBlank() &&
        remember(body) { body.encodeToByteArray().size <= MAX_SCRIPT_BYTES }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(back) { Text("Back") }
        Text("Script editor", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
        OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth(), label = { Text("Script") }, minLines = 7)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    try {
                        model.scripts.save(draft)
                        status = "Script saved"
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        status = "Could not save the script"
                    }
                }
            }, enabled = valid) { Text("Save") }
            OutlinedButton(onClick = {
                id = UUID.randomUUID().toString()
                title = ""
                body = ""
                status = ""
            }) { Text("New") }
            if (live != null) Button({ live(draft) }, enabled = valid) { Text("Go live") }
        }
        if (title.length > 200) Text("Use 200 characters or fewer for the title.")
        if (body.isNotBlank() && !valid && title.length <= 200) Text("Script exceeds the 2 MB limit.")
        if (status.isNotBlank()) Text(status)
        Text("Saved scripts", style = MaterialTheme.typography.titleLarge)
        if (saved.isEmpty()) Text("No saved scripts")
        saved.forEach { script ->
            Row {
                TextButton(onClick = {
                    id = script.id
                    title = script.title
                    body = script.text
                    status = ""
                }, modifier = Modifier.weight(1f)) { Text(script.title) }
                TextButton({ pendingDelete = script }) { Text("Delete") }
            }
        }
    }
    pendingDelete?.let { script ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this saved script?") },
            text = { Text(script.title) },
            confirmButton = {
                TextButton({
                    pendingDelete = null
                    scope.launch {
                        try {
                            model.scripts.delete(script)
                            if (id == script.id) id = UUID.randomUUID().toString()
                            status = "Script deleted"
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            status = "Could not delete the script"
                        }
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton({ pendingDelete = null }) { Text("Cancel") } },
        )
    }
}
