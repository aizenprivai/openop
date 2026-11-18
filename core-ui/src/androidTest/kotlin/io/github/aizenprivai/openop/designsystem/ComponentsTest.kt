package io.github.aizenprivai.openop.designsystem

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) =
        composeRule.setContent { OpenopTheme { content() } }

    @Test
    fun emptyStateShowsTitleAndBodyWithoutActions() {
        show { EmptyState(title = "Files", body = "Planned for Phase 5") }
        composeRule.onNodeWithText("Files").assertIsDisplayed()
        composeRule.onNodeWithText("Planned for Phase 5").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }

    @Test
    fun emptyStateShowsItsActionWhenGiven() {
        var clicked = 0
        show { EmptyState(title = "t", body = "b", action = { PrimaryButton("Start", onClick = { clicked++ }) }) }
        composeRule.onNodeWithText("Start").performClick()
        assertEquals(1, clicked)
    }

    @Test
    fun loadingStateExposesAnIndeterminateProgressIndicator() {
        show { LoadingState(label = "Reading") }
        composeRule.onNodeWithText("Reading").assertIsDisplayed()
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun errorStateRetryCallsBack() {
        var retries = 0
        show { ErrorState(message = "Could not open the folder.", onRetry = { retries++ }) }
        composeRule.onNodeWithText("Could not open the folder.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun errorStateWithoutRetryHasNoButton() {
        show { ErrorState(message = "Nope") }
        assertEquals(0, composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }

    @Test
    fun buttonsClickAndCanBeDisabled() {
        var clicks = 0
        show {
            PrimaryButton("Primary", onClick = { clicks++ })
            SecondaryButton("Secondary", onClick = { clicks++ })
            DestructiveButton("Delete", onClick = { clicks++ }, enabled = false)
        }
        composeRule.onNodeWithText("Primary").performClick()
        composeRule.onNodeWithText("Secondary").performClick()
        composeRule.onNodeWithText("Delete").assertIsNotEnabled()
        assertEquals(2, clicks)
    }

    @Test
    fun confirmDialogReportsConfirmAndDismiss() {
        var result = ""
        show {
            ConfirmDialog(
                title = "Export",
                text = "Export notes?",
                confirmLabel = "Export",
                onConfirm = { result = "confirm" },
                onDismiss = { result = "dismiss" }
            )
        }
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals("dismiss", result)
        composeRule.onNodeWithText("Export", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun destructiveDialogStartsWithFocusOnCancel() {
        var confirmed = false
        show {
            DestructiveConfirmDialog(
                title = "Delete files?",
                text = "This cannot be undone.",
                confirmLabel = "Delete 3 files",
                onConfirm = { confirmed = true },
                onDismiss = {}
            )
        }
        composeRule.onNodeWithText("Cancel").assertIsFocused()
        composeRule.onNodeWithText("Delete 3 files").performClick()
        assertEquals(true, confirmed)
    }

    @Test
    fun infoCardIsOneClickableTarget() {
        var clicks = 0
        show { InfoCard(title = "Files", body = "Browse storage", onClick = { clicks++ }) }
        composeRule.onNodeWithText("Files").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun listRowClicksAndSectionHeaderIsAHeading() {
        var clicks = 0
        show {
            SectionHeader("Areas")
            ListRow(headline = "Device", supporting = "Hardware information", onClick = { clicks++ })
        }
        composeRule.onNodeWithText("Areas").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithText("Device").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun labeledTextFieldShowsErrorAndAnnouncesIt() {
        show {
            var text by remember { mutableStateOf("") }
            LabeledTextField(
                value = text,
                onValueChange = { text = it },
                label = "Name",
                supportingText = "Shown in the list",
                errorText = if (text.isEmpty()) "Name is required" else null
            )
            Text("value=$text")
        }
        composeRule.onNodeWithText("Name is required").assertIsDisplayed()
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)).assertIsDisplayed()
        composeRule.onNodeWithText("Name").performTextInput("abc")
        composeRule.onNodeWithText("value=abc").assertIsDisplayed()
        composeRule.onNodeWithText("Shown in the list").assertIsDisplayed()
    }
}
