package io.github.aizenprivai.openop.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.aizenprivai.openop.MainActivity
import io.github.aizenprivai.openop.R
import io.github.aizenprivai.openop.core.CoreBridge
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaceholderScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun placeholderSaysNothingIsImplemented() {
        val body = composeRule.activity.getString(R.string.placeholder_body)
        composeRule.onNodeWithText(body).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun placeholderShowsRustCoreVersion() {
        val info = CoreBridge.coreInfo().getOrThrow()
        val expected = composeRule.activity.getString(R.string.core_status_ok, info.coreVersion, info.buildProfile)

        composeRule.waitUntilExactlyOneExists(hasText(expected), timeoutMillis = 5_000)
        composeRule.onNodeWithText(expected).assertIsDisplayed()
    }
}
