package io.github.aizenprivai.openop.navigation

import io.github.aizenprivai.openop.navigation.Destination.Ai
import io.github.aizenprivai.openop.navigation.Destination.Dashboard
import io.github.aizenprivai.openop.navigation.Destination.Device
import io.github.aizenprivai.openop.navigation.Destination.Files
import io.github.aizenprivai.openop.navigation.Destination.More
import io.github.aizenprivai.openop.navigation.Destination.Notes
import io.github.aizenprivai.openop.navigation.Destination.Tools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

/** Rules from docs/phase-2-plan.md §4. */
class NavStateTest {
    private val start = NavState.Start

    @Test
    fun startIsDashboardWithEveryTabAtItsRoot() {
        assertEquals(TopLevel.DASHBOARD, start.selected)
        assertEquals(listOf(Dashboard), start.current)
        TopLevel.entries.forEach { tab -> assertEquals(listOf(tab.root), start.stacks[tab]) }
    }

    @Test
    fun selectingAnotherTabSwitchesToIt() {
        val state = start.select(TopLevel.FILES)
        assertEquals(TopLevel.FILES, state.selected)
        assertEquals(listOf(Files), state.current)
    }

    @Test
    fun eachTabKeepsItsOwnStackWhenSwitchingAway() {
        val state = start.select(TopLevel.MORE).open(Device).select(TopLevel.NOTES).select(TopLevel.MORE)
        assertEquals(listOf(More, Device), state.current)
    }

    @Test
    fun reselectingTheCurrentTabResetsItToItsRoot() {
        val state = start.open(Device).select(TopLevel.MORE)
        assertEquals(TopLevel.MORE, state.selected)
        assertEquals(listOf(More), state.current)
    }

    @Test
    fun openingDeviceSelectsMoreWithDeviceOnTop() {
        val state = start.open(Device)
        assertEquals(TopLevel.MORE, state.selected)
        assertEquals(listOf(More, Device), state.current)
    }

    @Test
    fun openingToolsWhileOnDeviceReplacesDevice() {
        assertEquals(listOf(More, Tools), start.open(Device).open(Tools).current)
    }

    @Test
    fun openingTheCurrentRailDestinationDoesNothing() {
        val onDevice = start.open(Device)
        assertSame(onDevice, onDevice.open(Device))
        val onFiles = start.open(Files)
        assertSame(onFiles, onFiles.open(Files))
    }

    @Test
    fun openingATabRootSwitchesTabAndKeepsItsStack() {
        val state = start.open(Device).open(Ai).open(Tools).open(Notes)
        assertEquals(TopLevel.NOTES, state.selected)
        assertEquals(listOf(More, Tools), state.stacks[TopLevel.MORE])
    }

    @Test
    fun backPopsWithinTheCurrentTab() {
        val state = start.open(Device).back()
        assertEquals(TopLevel.MORE, state?.selected)
        assertEquals(listOf(More), state?.current)
    }

    @Test
    fun backAtTheRootOfAnotherTabGoesToDashboardAndKeepsThatTabsStack() {
        val state = start.open(Device).back()?.back()
        assertEquals(TopLevel.DASHBOARD, state?.selected)
        assertEquals(listOf(More), state?.stacks?.get(TopLevel.MORE))
    }

    @Test
    fun backAtTheDashboardRootExitsTheApp() {
        assertNull(start.back())
    }

    @Test
    fun railSelectionFollowsTheTopOfTheCurrentStack() {
        assertEquals(Dashboard, start.railSelection)
        assertEquals(Files, start.open(Files).railSelection)
        assertEquals(Device, start.open(Device).railSelection)
        // The More screen itself has no rail item.
        assertNull(start.select(TopLevel.MORE).railSelection)
    }

    @Test
    fun displayedStackKeepsDashboardUnderOtherTabsForBackPreview() {
        assertEquals(listOf(Dashboard), start.displayed)
        assertEquals(listOf(Dashboard, More, Device), start.open(Device).displayed)
    }

    @Test
    fun barAndRailListTheApprovedDestinations() {
        assertEquals(listOf(Dashboard, Files, Notes, Ai, More), NavState.BarDestinations)
        assertEquals(listOf(Dashboard, Files, Notes, Device, Tools, Ai), NavState.RailDestinations)
    }

    @Test
    fun openingTheRootOfTheCurrentDeepTabReturnsItToItsRoot() {
        // E.g. a Dashboard shortcut to More while Device is open: show More itself.
        assertEquals(listOf(More), start.open(Device).open(More).current)
    }

    @Test
    fun openingToolsDirectlySelectsMoreWithToolsOnTop() {
        assertEquals(listOf(More, Tools), start.open(Tools).current)
    }

    @Test
    fun backFromToolsGoesToMoreThenDashboardThenExits() {
        val more = start.open(Tools).back()
        assertEquals(listOf(More), more?.current)
        val dashboard = more?.back()
        assertEquals(TopLevel.DASHBOARD, dashboard?.selected)
        assertNull(dashboard?.back())
    }

    @Test
    fun reselectingATabAtItsRootKeepsTheSameState() {
        assertEquals(start, start.select(TopLevel.DASHBOARD))
        val files = start.select(TopLevel.FILES)
        assertEquals(files, files.select(TopLevel.FILES))
    }

    @Test
    fun openingAnotherTabKeepsTheDeepMoreStack() {
        val state = start.open(Device).open(Files)
        assertEquals(listOf(More, Device), state.stacks[TopLevel.MORE])
    }

    @Test
    fun displayedOnDashboardIgnoresOtherTabs() {
        assertEquals(listOf(Dashboard), start.open(Device).open(Dashboard).displayed)
    }

    @Test
    fun invalidStacksAreRejected() {
        val roots = TopLevel.entries.associateWith { listOf(it.root) }
        assertThrows(IllegalArgumentException::class.java) {
            NavState(TopLevel.MORE, roots + (TopLevel.MORE to listOf(Device)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            NavState(TopLevel.FILES, roots + (TopLevel.FILES to listOf(Files, Device)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            NavState(TopLevel.FILES, roots - TopLevel.FILES)
        }
    }

    @Test
    fun duplicateDestinationsInAStackAreRejected() {
        // Navigation 3 needs unique keys in the displayed list.
        val roots = TopLevel.entries.associateWith { listOf(it.root) }
        assertThrows(IllegalArgumentException::class.java) {
            NavState(TopLevel.MORE, roots + (TopLevel.MORE to listOf(More, Device, Device)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            NavState(TopLevel.MORE, roots + (TopLevel.MORE to listOf(More, More)))
        }
    }
}
