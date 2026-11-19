package io.github.aizenprivai.openop.shell

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.then
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.aizenprivai.openop.designsystem.OpenopTheme
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility Test Framework checks on every screen (decision P2-D8):
 * touch targets, contrast, labels, traversal order, text size. Also run at
 * font scale 2.0, in dark mode, and in a landscape window with the rail.
 * The checks need API 34+; on older devices these tests are skipped.
 */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun requireApi34() {
        assumeTrue("Accessibility checks need API 34+", Build.VERSION.SDK_INT >= 34)
        composeRule.enableAccessibilityChecks()
    }

    private fun show(type: NavigationSuiteType, override: DeviceConfigurationOverride? = null) =
        composeRule.setContent {
            val app = @androidx.compose.runtime.Composable {
                OpenopTheme { OpenopApp(coreStatus = "Rust core 0.1.0 (debug)", navigationSuiteType = type) }
            }
            if (override != null) DeviceConfigurationOverride(override) { app() } else app()
        }

    /** Visits every destination; the checks run before each click and once per screen. */
    private fun checkEveryScreen(items: List<String>) {
        composeRule.onRoot().tryPerformAccessibilityChecks()
        items.forEach { label ->
            composeRule.onNode(navItem(label)).performClick()
            composeRule.onRoot().tryPerformAccessibilityChecks()
        }
    }

    private val barItems = listOf("Files", "Notes", "AI", "More", "Dashboard")
    private val railItems = listOf("Files", "Notes", "Device", "Tools", "AI", "Dashboard")

    @Test
    fun barScreensPassChecks() {
        show(NavigationSuiteType.ShortNavigationBarCompact)
        checkEveryScreen(barItems)
        // The More screen's rows lead to Device and Tools.
        composeRule.onNode(navItem("More")).performClick()
        composeRule.onNode(hasText("Device") and nonNavClickable).performClick()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun railScreensPassChecks() {
        show(NavigationSuiteType.WideNavigationRailCollapsed)
        checkEveryScreen(railItems)
    }

    @Test
    fun darkModePassesChecks() {
        show(NavigationSuiteType.ShortNavigationBarCompact, DeviceConfigurationOverride.DarkMode(true))
        checkEveryScreen(barItems)
    }

    @Test
    fun fontScale200PercentPassesChecksAndKeepsNavigationUsable() {
        show(NavigationSuiteType.ShortNavigationBarCompact, DeviceConfigurationOverride.FontScale(2f))
        checkEveryScreen(barItems)
        barItems.forEach { composeRule.onNode(navItem(it)).assertIsDisplayed() }
    }

    @Test
    fun landscapeWindowWithRailAtLargeFontPassesChecks() {
        show(
            NavigationSuiteType.WideNavigationRailCollapsed,
            DeviceConfigurationOverride.ForcedSize(DpSize(840.dp, 400.dp)) then
                DeviceConfigurationOverride.FontScale(1.5f)
        )
        checkEveryScreen(railItems)
        railItems.forEach { composeRule.onNode(navItem(it)).assertIsDisplayed() }
    }
}
