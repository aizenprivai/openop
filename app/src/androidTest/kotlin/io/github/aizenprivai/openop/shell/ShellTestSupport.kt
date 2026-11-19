package io.github.aizenprivai.openop.shell

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
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
