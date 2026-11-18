package io.github.aizenprivai.openop.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Centred layout shared by the state components. */
@Composable
private fun CenteredState(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

/** Something is loading. [label] is shown under the indicator and read by screen readers. */
@Composable
fun LoadingState(modifier: Modifier = Modifier, label: String? = null) {
    val description = label ?: stringResource(R.string.openop_loading)
    CenteredState(modifier) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
        if (label != null) Text(label, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

/**
 * Nothing to show yet: an icon, a [title], an explanation and an optional
 * [action] (for example a button that starts something). Placeholders use it
 * without an action.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    action: (@Composable () -> Unit)? = null
) {
    CenteredState(modifier) {
        if (icon != null) {
            // Decorative: the title says what it means.
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        action?.invoke()
    }
}

/**
 * Something went wrong. [message] must be plain language for the user;
 * technical details belong in the log, never on screen.
 */
@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    CenteredState(modifier) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) SecondaryButton(text = stringResource(R.string.openop_action_retry), onClick = onRetry)
    }
}
