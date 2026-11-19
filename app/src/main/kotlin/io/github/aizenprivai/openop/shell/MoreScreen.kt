package io.github.aizenprivai.openop.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.aizenprivai.openop.designsystem.ListRow
import io.github.aizenprivai.openop.designsystem.R as DesignR
import io.github.aizenprivai.openop.designsystem.ScreenScaffold
import io.github.aizenprivai.openop.designsystem.visuals
import io.github.aizenprivai.openop.navigation.Destination

/** The "More" tab on phones: Device and Tools (decision P2-D1). */
@Composable
fun MoreScreen(onOpen: (Destination) -> Unit, modifier: Modifier = Modifier) {
    ScreenScaffold(title = stringResource(Destination.More.visuals.label), modifier = modifier) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            listOf(Destination.Device, Destination.Tools).forEach { destination ->
                val visuals = destination.visuals
                ListRow(
                    headline = stringResource(visuals.label),
                    supporting = visuals.plannedPhase?.let {
                        stringResource(DesignR.string.openop_planned_for_phase, it)
                    },
                    icon = painterResource(visuals.icon),
                    onClick = { onOpen(destination) }
                )
            }
        }
    }
}
