package dev.telepromptor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.telepromptor.bluetooth.VolumeShortcut
import dev.telepromptor.protocol.Script
import dev.telepromptor.protocol.SessionView
import dev.telepromptor.ui.home.HomeScreen
import dev.telepromptor.ui.host.HostPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ModeAndControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeInvokesBothModeSelections() {
        val selections = mutableListOf<String>()
        compose.setContent { MaterialTheme { HomeScreen({ selections += "host" }, { selections += "client" }, {}) } }
        compose.onNodeWithText("HOST").performClick()
        compose.onNodeWithText("CLIENT").performClick()
        compose.runOnIdle { assertEquals(listOf("host", "client"), selections) }
    }

    @Test fun playRestartAndFineAdjustUseHostCallbacks() {
        val state = mutableStateOf(SessionView(script = Script("1", "Demo", "A prompt")))
        var restart = false
        var plays = 0
        compose.setContent {
            MaterialTheme {
                HostPanel(
                    view = state.value,
                    countdown = false,
                    shortcut = VolumeShortcut.OFF,
                    back = {},
                    connect = {},
                    playPause = { plays++ },
                    restart = { restart = true },
                    seek = {},
                    update = { update ->
                        val current = state.value
                        state.value = current.copy(motion = current.motion.copy(state = update(current.motion.state)))
                    },
                    onCountdown = {},
                    onShortcut = {},
                    onTravel = {},
                )
            }
        }
        compose.onNodeWithText("Play").performClick()
        compose.onNodeWithText("Restart").performClick()
        compose.onNodeWithText("+").performScrollTo().performClick()
        compose.onNodeWithText("Scroll speed · 95 px/s").assertExists()
        compose.runOnIdle {
            assertTrue(restart)
            assertEquals(1, plays)
        }
    }
}
