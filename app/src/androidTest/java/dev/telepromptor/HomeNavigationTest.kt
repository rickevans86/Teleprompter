package dev.telepromptor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class HomeNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun bothModesOpenFromTheHomeScreen() {
        compose.onNodeWithText("HOST").performClick()
        compose.onNodeWithText("Script editor").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("CLIENT").performClick()
        compose.onNode(hasText("Bluetooth setup") or hasText("Connect to a host")).assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Telepromptor").assertIsDisplayed()
    }

    @Test
    fun settingsShowsInstalledVersionAndUpdateAction() {
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Installed version ${BuildConfig.VERSION_NAME}").assertIsDisplayed()
        compose.onNodeWithText("Update app").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Telepromptor").assertIsDisplayed()
    }
}
