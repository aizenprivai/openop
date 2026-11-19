package io.github.aizenprivai.openop.designsystem

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.aizenprivai.openop.navigation.Destination

/**
 * Honest placeholder for a module that is not implemented yet: its name, the
 * phase that will build it, and what it is for. No actions, so nothing looks
 * like a working feature (CLAUDE.md Phase 2 exit criterion).
 */
@Composable
fun AreaPlaceholder(destination: Destination, modifier: Modifier = Modifier) {
    val visuals = destination.visuals
    val title = stringResource(visuals.label)
    val purpose = stringResource(visuals.purpose)
    val body =
        visuals.plannedPhase?.let { "${stringResource(R.string.openop_planned_for_phase, it)}. $purpose" } ?: purpose
    ScreenScaffold(title = title, modifier = modifier) { padding ->
        EmptyState(
            title = title,
            body = body,
            icon = painterResource(visuals.icon),
            modifier = Modifier.padding(padding)
        )
    }
}
