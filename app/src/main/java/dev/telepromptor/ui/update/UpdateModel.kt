package dev.telepromptor.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.telepromptor.BuildConfig
import dev.telepromptor.update.AndroidUpdateActions
import dev.telepromptor.update.UpdateFlow
import dev.telepromptor.update.UpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One instance for the whole activity, so the startup banner and the Settings button share the same state. */
@HiltViewModel
class UpdateModel
    @Inject
    constructor(
        actions: AndroidUpdateActions,
    ) : ViewModel() {
        private val flow = UpdateFlow(actions)
        private val mutableState = MutableStateFlow<UpdateState>(UpdateState.Idle)
        val state: StateFlow<UpdateState> = mutableState.asStateFlow()

        init {
            // Debug builds are signed differently from releases, so offering a release to them would only mislead.
            if (!BuildConfig.DEBUG) {
                viewModelScope.launch {
                    val found = flow.startupCheck()
                    mutableState.update { current -> if (current == UpdateState.Idle) found else current }
                }
            }
        }

        fun tap() {
            viewModelScope.launch {
                mutableState.value = flow.tap(mutableState.value) { mutableState.value = it }
            }
        }

        fun dismiss() {
            mutableState.update { current ->
                when (current) {
                    is UpdateState.Available, is UpdateState.NeedPermission, is UpdateState.Installing -> UpdateState.Idle
                    else -> current
                }
            }
        }
    }
