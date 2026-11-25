package dev.telepromptor.ui.host

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.telepromptor.data.SavedScript
import dev.telepromptor.data.Scripts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ScriptEditorModel
    @Inject
    constructor(
        private val scripts: Scripts,
    ) : ViewModel() {
        val saved = scripts.observe()
        var id by mutableStateOf(UUID.randomUUID().toString())
        var title by mutableStateOf("")
        var body by mutableStateOf("")
        var status by mutableStateOf("")

        fun save(draft: SavedScript) {
            viewModelScope.launch {
                try {
                    scripts.save(draft)
                    status = "Script saved"
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    status = "Could not save the script"
                }
            }
        }

        fun delete(script: SavedScript) {
            viewModelScope.launch {
                try {
                    scripts.delete(script)
                    if (id == script.id) id = UUID.randomUUID().toString()
                    status = "Script deleted"
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    status = "Could not delete the script"
                }
            }
        }
    }
