package dev.telepromptor.ui.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.telepromptor.bluetooth.LiveSession
import dev.telepromptor.protocol.RetryDelay
import dev.telepromptor.protocol.Role
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class ClientConnection(
    val status: String? = "Select the phone running HOST mode.",
    val showDisplay: Boolean = false,
    val connecting: Boolean = false,
)

@HiltViewModel
class ClientModel
    @Inject
    constructor(
        val live: LiveSession,
    ) : ViewModel() {
        private val mutableConnection = MutableStateFlow(ClientConnection())
        val connection = mutableConnection.asStateFlow()
        private var connectionJob: Job? = null

        fun connect(address: String) {
            if (connectionJob?.isActive == true) return
            val session = live.session
            session.begin(Role.CLIENT)
            mutableConnection.value = ClientConnection("Connecting…", connecting = true)
            connectionJob =
                viewModelScope.launch {
                    val retry = RetryDelay()
                    var hasConnected = false
                    while (isActive) {
                        val peer = withTimeoutOrNull(20_000) { live.radio.connect(address) }
                        var negotiated = false
                        try {
                            if (peer != null) {
                                val ready =
                                    withTimeoutOrNull(7_000) {
                                        session.view.first { peer in it.peers || it.removedByHost }
                                    }
                                if (ready?.removedByHost == true) {
                                    mutableConnection.value = ClientConnection("Disconnected by host")
                                    session.endIf(Role.CLIENT)
                                    return@launch
                                }
                                negotiated = ready != null
                            }
                            if (peer != null && negotiated) {
                                hasConnected = true
                                retry.connected()
                                mutableConnection.value = ClientConnection(status = null, showDisplay = true)
                                val lost = session.view.first { peer !in it.peers || it.removedByHost }
                                if (lost.removedByHost) {
                                    mutableConnection.value = ClientConnection("Disconnected by host")
                                    session.endIf(Role.CLIENT)
                                    return@launch
                                }
                            }
                        } finally {
                            if (peer != null && !negotiated) live.radio.close(peer)
                        }
                        if (!hasConnected) {
                            mutableConnection.value = ClientConnection("Could not connect. Check the host and try again.")
                            session.endIf(Role.CLIENT)
                            return@launch
                        }
                        mutableConnection.value = ClientConnection("Connection lost. Reconnecting…", showDisplay = true)
                        delay(retry.afterFailure())
                    }
                }
        }

        fun disconnect() {
            connectionJob?.cancel()
            connectionJob = null
            live.session.endIf(Role.CLIENT)
            mutableConnection.value = ClientConnection("Disconnected")
        }

        override fun onCleared() {
            disconnect()
            super.onCleared()
        }
    }
