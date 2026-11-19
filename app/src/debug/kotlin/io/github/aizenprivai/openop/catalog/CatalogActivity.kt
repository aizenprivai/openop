package io.github.aizenprivai.openop.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.aizenprivai.openop.designsystem.ConfirmDialog
import io.github.aizenprivai.openop.designsystem.DestructiveButton
import io.github.aizenprivai.openop.designsystem.DestructiveConfirmDialog
import io.github.aizenprivai.openop.designsystem.EmptyState
import io.github.aizenprivai.openop.designsystem.ErrorState
import io.github.aizenprivai.openop.designsystem.InfoCard
import io.github.aizenprivai.openop.designsystem.LabeledTextField
import io.github.aizenprivai.openop.designsystem.ListRow
import io.github.aizenprivai.openop.designsystem.LoadingState
import io.github.aizenprivai.openop.designsystem.OpenopTheme
import io.github.aizenprivai.openop.designsystem.PrimaryButton
import io.github.aizenprivai.openop.designsystem.ScreenScaffold
import io.github.aizenprivai.openop.designsystem.SecondaryButton
import io.github.aizenprivai.openop.designsystem.SectionHeader
import io.github.aizenprivai.openop.designsystem.Spacing

/**
 * Debug-only catalogue of every design-system component, for reviewing the
 * design on a device without Android Studio (decision P2-D10). Not in release
 * builds: this file lives in the debug source set.
 */
class CatalogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Catalog() }
    }
}

@Composable
private fun Catalog() {
    val systemDark = isSystemInDarkTheme()
    var dark by rememberSaveable { mutableStateOf(systemDark) }
    var dialog by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }

    OpenopTheme(darkTheme = dark) {
        ScreenScaffold(
            title = "Component catalogue",
            actions = {
                Text(if (dark) "Dark" else "Light", modifier = Modifier.padding(end = Spacing.s))
                Switch(
                    checked = dark,
                    onCheckedChange = { dark = it },
                    modifier = Modifier.semantics { contentDescription = "Dark theme" }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.m),
                verticalArrangement = Arrangement.spacedBy(Spacing.s)
            ) {
                SectionHeader("Buttons")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimaryButton("Primary", onClick = {})
                    SecondaryButton("Secondary", onClick = {})
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    DestructiveButton("Delete", onClick = { dialog = "destructive" })
                    PrimaryButton("Disabled", onClick = {}, enabled = false)
                }

                SectionHeader("Dialogs")
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    SecondaryButton("Confirm dialog", onClick = { dialog = "confirm" })
                    SecondaryButton("Destructive dialog", onClick = { dialog = "destructive" })
                }

                SectionHeader("Cards")
                InfoCard(title = "Info card", body = "With a body and a click action", onClick = {})
                InfoCard(title = "Info card without action")

                SectionHeader("Lists")
                ListRow(headline = "List row", supporting = "Supporting text", onClick = {})
                ListRow(headline = "List row without supporting text")

                SectionHeader("Inputs")
                LabeledTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = "Name",
                    supportingText = "Supporting text",
                    errorText = if (text == "error") "Error text (type anything else to clear)" else null
                )

                SectionHeader("States")
                StateBox { EmptyState(title = "Empty state", body = "Nothing to show yet.") }
                StateBox { LoadingState(label = "Loading state") }
                StateBox { ErrorState(message = "Error state with a plain-language message.", onRetry = {}) }
            }
        }

        when (dialog) {
            "confirm" -> ConfirmDialog(
                title = "Confirm dialog",
                text = "A non-destructive confirmation.",
                confirmLabel = "Continue",
                onConfirm = { dialog = "" },
                onDismiss = { dialog = "" }
            )

            "destructive" -> DestructiveConfirmDialog(
                title = "Delete 3 files?",
                text = "This cannot be undone. Focus starts on Cancel.",
                confirmLabel = "Delete 3 files",
                onConfirm = { dialog = "" },
                onDismiss = { dialog = "" }
            )
        }
    }
}

@Composable
private fun StateBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().height(220.dp)) { content() }
}
