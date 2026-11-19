package io.github.aizenprivai.openop.shell

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(expected))
        composeRule.waitUntil(timeoutMillis = 5_000) { composeRule.isShowing(expected) }
    }

    @Test
    fun navigationSurvivesActivityRecreation() {
        composeRule.onNode(navItem("Notes")).performClick()
        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 5_000) { composeRule.isShowing("Planned for Phase 6") }
        assertTrue(composeRule.isShowing("Planned for Phase 6"))
    }
}
