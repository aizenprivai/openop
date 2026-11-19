package io.github.aizenprivai.openop.shell

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import io.github.aizenprivai.openop.designsystem.visuals
import io.github.aizenprivai.openop.feature.ai.AiScreen
import io.github.aizenprivai.openop.feature.dashboard.DashboardScreen
import io.github.aizenprivai.openop.feature.device.DeviceScreen
import io.github.aizenprivai.openop.feature.files.FilesScreen
import io.github.aizenprivai.openop.feature.notes.NotesScreen
import io.github.aizenprivai.openop.feature.tools.ToolsScreen
import io.github.aizenprivai.openop.navigation.Destination
import io.github.aizenprivai.openop.navigation.NavState
import io.github.aizenprivai.openop.navigation.NavStateCodec

/** Saves [NavState] across recreation and process death as text (see [NavStateCodec]). */
internal val NavStateSaver: Saver<NavState, String> =
    Saver(save = { NavStateCodec.encode(it) }, restore = { NavStateCodec.decode(it) })

private val BarTypes = setOf(
    NavigationSuiteType.ShortNavigationBarCompact,
    NavigationSuiteType.ShortNavigationBarMedium,
    NavigationSuiteType.NavigationBar
)

/**
 * The app shell: bottom bar on phones (Dashboard, Files, Notes, AI, More),
 * navigation rail with all six areas on wide windows (decision P2-D1), and the
 * screen for the current destination. Navigation rules live in [NavState]
 * (docs/phase-2-plan.md §4).
 *
 * @param navigationSuiteType bar or rail; computed from the window by default,
 *   settable in tests.
 * @param coreStatus footer line for the Dashboard (Rust core status).
 */
@Composable
fun OpenopApp(
    coreStatus: String?,
    modifier: Modifier = Modifier,
    navigationSuiteType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.navigationSuiteType(currentWindowAdaptiveInfoV2())
) {
    var nav by rememberSaveable(stateSaver = NavStateSaver) { mutableStateOf(NavState.Start) }
    // NavDisplay keeps each entry's content lambda, so read changing inputs through
    // State: the Dashboard footer must update when the Rust call returns.
    val currentCoreStatus by rememberUpdatedState(coreStatus)
    val isBar = navigationSuiteType in BarTypes

    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteType = navigationSuiteType,
        navigationItems = {
            val items = if (isBar) NavState.BarDestinations else NavState.RailDestinations
            items.forEach { destination ->
                val selected = if (isBar) nav.selected == destination.tab else nav.railSelection == destination
                val visuals = destination.visuals
                NavigationSuiteItem(
                    selected = selected,
                    onClick = { nav = if (isBar) nav.select(destination.tab) else nav.open(destination) },
                    icon = {
                        // Decorative: the label names the item.
                        Icon(
                            painterResource(if (selected) visuals.selectedIcon else visuals.icon),
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(visuals.label)) },
                    navigationSuiteType = navigationSuiteType
                )
            }
        }
    ) {
        NavDisplay(
            backStack = nav.displayed,
            onBack = { nav.back()?.let { nav = it } },
            entryProvider = { destination ->
                NavEntry(destination) {
                    when (destination) {
                        Destination.Dashboard -> DashboardScreen(onOpen = {
                            nav = nav.open(it)
                        }, footer = currentCoreStatus)

                        Destination.Files -> FilesScreen()

                        Destination.Notes -> NotesScreen()

                        Destination.Ai -> AiScreen()

                        Destination.More -> MoreScreen(onOpen = { nav = nav.open(it) })

                        Destination.Device -> DeviceScreen()

                        Destination.Tools -> ToolsScreen()
                    }
                }
            }
        )
    }
}
