package io.github.aizenprivai.openop.shell

import androidx.activity.ComponentActivity
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Wide-window layout: navigation rail with all six areas (decision P2-D1). */
@RunWith(AndroidJUnit4::class)
class ShellRailTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun showShell() = composeRule.setContent {
        OpenopApp(coreStatus = null, navigationSuiteType = NavigationSuiteType.WideNavigationRailCollapsed)
    }

    private fun tap(label: String) = composeRule.onNode(navItem(label)).performClick()

    @Test
    fun railShowsAllSixAreasWithoutMore() {
        showShell()
        assertEquals(listOf("Dashboard", "Files", "Notes", "Device", "Tools", "AI"), composeRule.navItemLabels())
    }

    @Test
    fun railOpensDeviceAndToolsDirectly() {
        showShell()
        tap("Device")
        composeRule.onNode(navItem("Device")).assertIsSelected()
        assertTrue(composeRule.isShowingExactly(Placeholder.DEVICE))
        tap("Tools")
        composeRule.onNode(navItem("Tools")).assertIsSelected()
        composeRule.onNode(navItem("Device")).assertIsNotSelected()
        assertTrue(composeRule.isShowingExactly(Placeholder.TOOLS))
    }

    @Test
    fun reselectingTheCurrentRailItemChangesNothing() {
        showShell()
        tap("Device")
        tap("Device")
        composeRule.onNode(navItem("Device")).assertIsSelected()
        assertTrue(composeRule.isShowingExactly(Placeholder.DEVICE))
    }

    @Test
    fun backFromDeviceShowsTheMoreScreenThenDashboard() {
        showShell()
        tap("Device")
        composeRule.pressBack()
        assertFalse(composeRule.isShowingExactly(Placeholder.DEVICE))
        assertTrue("More screen lists Tools", composeRule.isShowing("Planned for Phase 8"))
        composeRule.pressBack()
        composeRule.onNode(navItem("Dashboard")).assertIsSelected()
    }
}
