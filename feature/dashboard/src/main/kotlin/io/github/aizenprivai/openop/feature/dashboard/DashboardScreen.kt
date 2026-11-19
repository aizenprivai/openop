package io.github.aizenprivai.openop.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.aizenprivai.openop.designsystem.InfoCard
import io.github.aizenprivai.openop.designsystem.R as DesignR
import io.github.aizenprivai.openop.designsystem.ScreenScaffold
import io.github.aizenprivai.openop.designsystem.SectionHeader
import io.github.aizenprivai.openop.designsystem.Spacing
import io.github.aizenprivai.openop.designsystem.visuals
import io.github.aizenprivai.openop.navigation.Destination

/** The areas the Dashboard links to (CLAUDE.md §5.1: entry points to every module). */
private val Shortcuts =
    listOf(Destination.Files, Destination.Notes, Destination.Device, Destination.Tools, Destination.Ai)

/**
 * Phase 2 Dashboard: shortcuts to every module (these navigate for real) and a
 * footer line from the app (the Rust core status). The real overview is Phase 4.
 */
@Composable
fun DashboardScreen(onOpen: (Destination) -> Unit, footer: String?, modifier: Modifier = Modifier) {
    ScreenScaffold(title = stringResource(Destination.Dashboard.visuals.label), modifier = modifier) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Spacing.m),
            verticalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            item {
                Text(
                    stringResource(R.string.dashboard_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item { SectionHeader(stringResource(R.string.dashboard_areas)) }
            items(Shortcuts, key = { it.id }) { destination ->
                val visuals = destination.visuals
                InfoCard(
                    title = stringResource(visuals.label),
                    body = visuals.plannedPhase?.let { stringResource(DesignR.string.openop_planned_for_phase, it) },
                    icon = painterResource(visuals.icon),
                    onClick = { onOpen(destination) }
                )
            }
            if (footer != null) {
                item {
                    Text(
                        footer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.m)
                    )
                }
            }
        }
    }
}
