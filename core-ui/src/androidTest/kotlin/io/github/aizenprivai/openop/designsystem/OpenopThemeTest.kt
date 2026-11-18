package io.github.aizenprivai.openop.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenopThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun schemeFor(darkTheme: Boolean): ColorScheme {
        lateinit var scheme: ColorScheme
        composeRule.setContent { OpenopTheme(darkTheme = darkTheme) { scheme = MaterialTheme.colorScheme } }
        composeRule.waitForIdle()
        return scheme
    }

    @Test
    fun darkThemeUsesTheDynamicDarkScheme() {
        val scheme = schemeFor(darkTheme = true)
        assertEquals(dynamicDarkColorScheme(context).primary, scheme.primary)
        assertTrue("dark background expected", scheme.background.luminance() < 0.5f)
    }

    @Test
    fun lightThemeUsesTheDynamicLightScheme() {
        val scheme = schemeFor(darkTheme = false)
        assertEquals(dynamicLightColorScheme(context).primary, scheme.primary)
        assertTrue("light background expected", scheme.background.luminance() > 0.5f)
    }
}
