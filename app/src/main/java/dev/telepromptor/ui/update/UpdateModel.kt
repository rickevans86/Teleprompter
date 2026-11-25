package dev.telepromptor.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.telepromptor.BuildConfig
import dev.telepromptor.update.AvailableUpdate
import dev.telepromptor.update.UpdateChecker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateModel
    @Inject
    constructor(
        checker: UpdateChecker,
    ) : ViewModel() {
        private val mutableUpdate = MutableStateFlow<AvailableUpdate?>(null)
        val update: StateFlow<AvailableUpdate?> = mutableUpdate.asStateFlow()

        init {
            viewModelScope.launch { mutableUpdate.value = checker.check(BuildConfig.VERSION_NAME) }
        }

        fun dismiss() {
            mutableUpdate.value = null
        }
    }
