package io.github.aizenprivai.openop.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

/**
 * A card with a [title], optional [body], optional leading [icon] and trailing
 * content. With [onClick] the whole card is one touch target (e.g. a Dashboard
 * shortcut).
 */
@Composable
fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    icon: Painter? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = if (icon != null) Spacing.m else 0.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (body != null) {
                    Text(
                        body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailing?.invoke()
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth()) { content() }
    } else {
        Card(modifier = modifier.fillMaxWidth()) { content() }
    }
}
