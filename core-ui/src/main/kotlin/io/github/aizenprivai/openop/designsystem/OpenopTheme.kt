package io.github.aizenprivai.openop.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * OPENOP's Material 3 theme. Colours come from the wallpaper (Dynamic Color),
 * which every supported device has (minSdk 31). Light or dark follows the
 * system setting; a user override arrives with Settings in Phase 3.
 * Typography and shapes are Material 3 defaults with the system font.
 */
@Composable
fun OpenopTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = colorScheme, content = content)
}
