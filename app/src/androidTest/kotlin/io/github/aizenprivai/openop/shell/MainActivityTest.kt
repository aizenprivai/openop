package io.github.aizenprivai.openop.shell

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.aizenprivai.openop.MainActivity
import io.github.aizenprivai.openop.R
import io.github.aizenprivai.openop.core.CoreBridge
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The real activity: Rust core footer and navigation across recreation. */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardFooterShowsTheRustCoreVersion() {
        val info = CoreBridge.coreInfo().getOrThrow()
        val expected = composeRule.activity.getString(R.string.core_status_ok, info.coreVersion, info.buildProfile)
        // The footer is the last row; its text changes once the Rust call (off the
        // main thread) returns, so scroll to the end first, then wait for the text.
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(DASHBOARD_LAST_ROW)
        composeRule.waitUntil(timeoutMillis = 10_000) { composeRule.isShowingExactly(expected) }
    }

    @Test
    fun navigationSurvivesActivityRecreation() {
        composeRule.onNode(navItem("Notes")).performClick()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 5_000) { composeRule.isShowingExactly(Placeholder.NOTES) }
        composeRule.onNode(navItem("Notes")).assertIsSelected()
        assertTrue(composeRule.isShowingExactly(Placeholder.NOTES))
    }
}

/** Dashboard rows: intro, section header, five shortcuts, footer. */
private const val DASHBOARD_LAST_ROW = 7
