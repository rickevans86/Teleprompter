package dev.telepromptor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import dagger.hilt.android.AndroidEntryPoint
import dev.telepromptor.ui.home.HomeScreen
import dev.telepromptor.ui.host.ScriptEditor

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
        composable("host") { ScriptEditor(back = { nav.popBackStack() }) }
        composable("client") { Button(onClick = { nav.popBackStack() }) { Text("Back") } }
    }
}
