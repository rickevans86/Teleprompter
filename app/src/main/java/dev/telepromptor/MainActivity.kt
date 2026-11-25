package dev.telepromptor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import dev.telepromptor.bluetooth.LiveSession
import dev.telepromptor.ui.client.ClientRoute
import dev.telepromptor.ui.home.HomeScreen
import dev.telepromptor.ui.host.HostRoute
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
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen({ nav.navigate("host") }, { nav.navigate("client") }) }
        composable("host") { HostRoute(back = { nav.popBackStack() }) }
        composable("client") { ClientRoute(back = { nav.popBackStack() }) }
    }
}
