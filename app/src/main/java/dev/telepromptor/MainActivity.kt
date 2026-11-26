package dev.telepromptor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import dev.telepromptor.bluetooth.LiveSession
import dev.telepromptor.ui.client.ClientRoute
import dev.telepromptor.ui.home.HomeScreen
import dev.telepromptor.ui.host.HostRoute
import dev.telepromptor.ui.settings.SettingsScreen
import dev.telepromptor.ui.update.UpdateModel
import dev.telepromptor.update.UpdateState
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var live: LiveSession

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (live.handleVolumeKey(event)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface { TelepromptorNavigation() }
            }
        }
    }
}

@Composable
fun TelepromptorNavigation() {
    val nav = rememberNavController()
    // Created here, above the NavHost, so the home banner and the Settings button share one update state.
    val updates: UpdateModel = hiltViewModel()
    val updateState: UpdateState by updates.state.collectAsStateWithLifecycle()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                host = { nav.navigate("host") },
                client = { nav.navigate("client") },
                settings = { nav.navigate("settings") },
                update = updateState,
                onUpdate = updates::tap,
                dismissUpdate = updates::dismiss,
            )
        }
        composable("settings") { SettingsScreen(back = { nav.popBackStack() }, update = updateState, onUpdate = updates::tap) }
        composable("host") { HostRoute(back = { nav.popBackStack() }) }
        composable("client") { ClientRoute(back = { nav.popBackStack() }) }
    }
}
