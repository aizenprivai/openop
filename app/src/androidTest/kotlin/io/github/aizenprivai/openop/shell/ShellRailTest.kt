package io.github.aizenprivai.openop.shell

import androidx.activity.ComponentActivity
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
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
        assertTrue(composeRule.isShowing("Planned for Phase 4. Information about this Android device."))
        tap("Tools")
        composeRule.onNode(navItem("Tools")).assertIsSelected()
        composeRule.onNode(navItem("Device")).assertIsNotSelected()
        assertTrue(composeRule.isShowing("Planned for Phase 8"))
    }

    @Test
    fun reselectingTheCurrentRailItemChangesNothing() {
        showShell()
        tap("Device")
        tap("Device")
        composeRule.onNode(navItem("Device")).assertIsSelected()
        assertTrue(composeRule.isShowing("Information about this Android device."))
    }

    @Test
    fun backFromDeviceShowsTheMoreScreenThenDashboard() {
        showShell()
        tap("Device")
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        assertTrue("More screen lists Tools", composeRule.isShowing("Planned for Phase 8"))
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNode(navItem("Dashboard")).assertIsSelected()
    }
}
