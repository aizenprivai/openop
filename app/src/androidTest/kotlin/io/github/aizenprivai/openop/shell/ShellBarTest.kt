package io.github.aizenprivai.openop.shell

import androidx.activity.ComponentActivity
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Phone layout: bottom bar. Rules from docs/phase-2-plan.md §4. */
@RunWith(AndroidJUnit4::class)
class ShellBarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun showShell() = composeRule.setContent {
        OpenopApp(coreStatus = "core ok", navigationSuiteType = NavigationSuiteType.ShortNavigationBarCompact)
    }

    private fun tap(label: String) = composeRule.onNode(navItem(label)).performClick()

    private fun back() = composeRule.pressBack()

    @Test
    fun barShowsTheFiveApprovedItemsInOrder() {
        showShell()
        assertEquals(listOf("Dashboard", "Files", "Notes", "AI", "More"), composeRule.navItemLabels())
        composeRule.onNode(navItem("Dashboard")).assertIsSelected()
    }

    @Test
    fun everyBarItemOpensItsPlaceholder() {
        showShell()
        listOf(
            "Files" to Placeholder.FILES,
            "Notes" to Placeholder.NOTES,
            "AI" to Placeholder.AI
        ).forEach { (label, text) ->
            tap(label)
            composeRule.onNode(navItem(label)).assertIsSelected()
            assertTrue("$label placeholder", composeRule.isShowingExactly(text))
        }
    }

    @Test
    fun placeholdersOfferNoActions() {
        showShell()
        listOf("Files", "Notes", "AI").forEach { label ->
            tap(label)
            assertEquals("$label has actions", 0, composeRule.onAllNodes(nonNavClickable).fetchSemanticsNodes().size)
        }
    }

    @Test
    fun moreListsDeviceAndToolsAndOpensThem() {
        showShell()
        tap("More")
        composeRule.onNode(hasText("Device") and nonNavClickable).performClick()
        assertTrue(composeRule.isShowingExactly(Placeholder.DEVICE))
        back()
        assertFalse(composeRule.isShowingExactly(Placeholder.DEVICE))
        composeRule.onNode(hasText("Tools") and nonNavClickable).performClick()
        assertTrue(composeRule.isShowingExactly(Placeholder.TOOLS))
    }

    @Test
    fun backFromATabRootGoesToDashboardAndBackOnDashboardLeaves() {
        showShell()
        tap("Files")
        back()
        composeRule.onNode(navItem("Dashboard")).assertIsSelected()
        assertFalse(composeRule.activity.isFinishing)
        back()
        assertTrue("back on the Dashboard root leaves the app", composeRule.activity.isFinishing)
    }

    @Test
    fun reselectingMoreReturnsItToItsRoot() {
        showShell()
        tap("More")
        composeRule.onNode(hasText("Device") and nonNavClickable).performClick()
        tap("More")
        composeRule.onNode(hasText("Tools") and nonNavClickable).assertExists()
        assertFalse(composeRule.isShowingExactly(Placeholder.DEVICE))
    }

    @Test
    fun eachTabKeepsItsPlaceWhenSwitching() {
        showShell()
        tap("More")
        composeRule.onNode(hasText("Device") and nonNavClickable).performClick()
        tap("Files")
        assertTrue(composeRule.isShowingExactly(Placeholder.FILES))
        // Coming back to More shows Device again; it was not reset.
        composeRule.onNode(navItem("More")).performClick()
        assertTrue(composeRule.isShowingExactly(Placeholder.DEVICE))
    }

    @Test
    fun dashboardShortcutsOpenTheirArea() {
        showShell()
        composeRule.onNode(hasText("Device") and nonNavClickable).performClick()
        composeRule.onNode(navItem("More")).assertIsSelected()
        assertTrue(composeRule.isShowingExactly(Placeholder.DEVICE))
    }

    @Test
    fun navigationSurvivesSaveAndRestore() {
        // Same save/restore path Android uses after process death.
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            OpenopApp(coreStatus = null, navigationSuiteType = NavigationSuiteType.ShortNavigationBarCompact)
        }
        tap("More")
        composeRule.onNode(hasText("Tools") and nonNavClickable).performClick()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNode(navItem("More")).assertIsSelected()
        // [More, Tools] restored, not just [More]: the Tools screen itself is shown.
        assertTrue(composeRule.isShowingExactly(Placeholder.TOOLS))
    }
}
