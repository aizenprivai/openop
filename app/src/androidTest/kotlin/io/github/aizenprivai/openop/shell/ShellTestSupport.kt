package io.github.aizenprivai.openop.shell

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule

/** A navigation bar or rail item: selectable, with this label. */
fun navItem(label: String): SemanticsMatcher = hasText(label) and isSelectable()

/** A clickable element that is not a navigation item (cards, rows, buttons). */
val nonNavClickable: SemanticsMatcher =
    hasClickAction() and !SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)

fun ComposeTestRule.navItemLabels(): List<String> = onAllNodes(isSelectable()).fetchSemanticsNodes().map { node ->
    node.config[SemanticsProperties.Text].joinToString("") { it.text }
}

fun ComposeTestRule.isShowing(text: String): Boolean =
    onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()

/**
 * Text that only a placeholder screen shows: "Planned for Phase N. <purpose>".
 * Dashboard cards and More rows show "Planned for Phase N" alone, so this
 * cannot match them.
 */
object Placeholder {
    const val FILES = "Planned for Phase 5. A file manager for shared storage, SD cards and USB drives."
    const val NOTES = "Planned for Phase 6. Local note-taking with Markdown and rich text."
    const val AI = "Planned for Phase 7. Run compatible AI models locally on this device."
    const val DEVICE = "Planned for Phase 4. Information about this Android device."
    const val TOOLS = "Planned for Phase 8. Practical offline utilities in one place."
}

/** True when exactly this full text is on screen (no substring matching). */
fun ComposeTestRule.isShowingExactly(text: String): Boolean =
    onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

/**
 * Presses Back the way the system does. Waits for pending recomposition first:
 * the v2 test rule queues it, and a back press before the new screen is
 * composed would reach the old screen's back handling (or none at all).
 */
fun AndroidComposeTestRule<*, out ComponentActivity>.pressBack() {
    waitForIdle()
    runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
    waitForIdle()
}
