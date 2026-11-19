package io.github.aizenprivai.openop.shell

import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.aizenprivai.openop.designsystem.OpenopTheme
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility Test Framework checks on every screen (decision P2-D8):
 * touch targets, contrast, labels, traversal order, text size. Also run at
 * font scale 2.0, in dark mode, and in landscape.
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
    fun landscapePhoneLayoutPassesChecks() {
        // A real rotation, not a forced size: forcing a size rescales density, which
        // distorts the checker's touch-target measurements. The app chooses bar or
        // rail itself, as on a device (compact height gets the bar).
        composeRule.runOnUiThread {
            composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        composeRule.waitForIdle()
        try {
            composeRule.setContent { OpenopTheme { OpenopApp(coreStatus = "Rust core 0.1.0 (debug)") } }
            composeRule.onRoot().tryPerformAccessibilityChecks()
            val labels = composeRule.navItemLabels()
            assertTrue(
                "navigation items present: $labels",
                labels.containsAll(listOf("Dashboard", "Files", "Notes", "AI"))
            )
            checkEveryScreen(labels.filter { it != "Dashboard" } + "Dashboard")
        } finally {
            composeRule.runOnUiThread {
                composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
}
