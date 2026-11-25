package dev.telepromptor.ui.host

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.telepromptor.data.SavedScript
import dev.telepromptor.protocol.MAX_SCRIPT_BYTES
import java.util.UUID

@Composable
fun ScriptEditor(
    back: () -> Unit,
    live: ((SavedScript) -> Unit)? = null,
    model: ScriptEditorModel = hiltViewModel(),
) {
    val saved by model.saved.collectAsStateWithLifecycle(emptyList())
    var pendingDelete by remember { mutableStateOf<SavedScript?>(null) }
    val draft = SavedScript(model.id, model.title.trim().ifBlank { "Untitled script" }, model.body)
    val valid =
        model.title.length <= 200 &&
            model.body.isNotBlank() &&
            remember(model.body) { model.body.encodeToByteArray().size <= MAX_SCRIPT_BYTES }
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(back) { Text("Back") }
        Text("Script editor", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(model.title, { model.title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
        OutlinedTextField(model.body, { model.body = it }, Modifier.fillMaxWidth(), label = { Text("Script") }, minLines = 7)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                model.save(draft)
            }, enabled = valid) { Text("Save") }
            OutlinedButton(onClick = {
                model.id = UUID.randomUUID().toString()
                model.title = ""
                model.body = ""
                model.status = ""
            }) { Text("New") }
            if (live != null) Button({ live(draft) }, enabled = valid) { Text("Go live") }
        }
        if (model.title.length > 200) Text("Use 200 characters or fewer for the title.")
        if (model.body.isNotBlank() && !valid && model.title.length <= 200) Text("Script exceeds the 2 MB limit.")
        if (model.status.isNotBlank()) Text(model.status)
        Text("Saved scripts", style = MaterialTheme.typography.titleLarge)
        if (saved.isEmpty()) Text("No saved scripts")
        saved.forEach { script ->
            Row {
                TextButton(onClick = {
                    model.id = script.id
                    model.title = script.title
                    model.body = script.text
                    model.status = ""
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
                    model.delete(script)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton({ pendingDelete = null }) { Text("Cancel") } },
        )
    }
}
