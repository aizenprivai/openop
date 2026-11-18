package io.github.aizenprivai.openop.navigation

/**
 * Immutable navigation state of the app shell: the selected tab and one back
 * stack per tab. Every operation returns a new state. Rules are documented in
 * docs/phase-2-plan.md §4.
 *
 * Invariant: every tab has a non-empty stack that starts with its root, and
 * contains only distinct destinations of that tab.
 */
data class NavState(val selected: TopLevel, val stacks: Map<TopLevel, List<Destination>>) {
    init {
        TopLevel.entries.forEach { tab ->
            val stack = stacks[tab]
            require(!stack.isNullOrEmpty() && stack.first() == tab.root) {
                "Stack for $tab must start with ${tab.root}"
            }
            require(stack.all { it.tab == tab }) { "Stack for $tab contains destinations of another tab" }
            // Navigation 3 needs unique keys in the displayed list.
            require(stack.distinct().size == stack.size) { "Stack for $tab contains duplicates" }
        }
    }

    /** Back stack of the selected tab. */
    val current: List<Destination> get() = stacks.getValue(selected)

    /**
     * What the navigation display shows: the current stack, with the Dashboard
     * stack underneath when another tab is selected, so the back gesture
     * previews where back goes ("exit through home").
     */
    val displayed: List<Destination>
        get() = if (selected == TopLevel.DASHBOARD) current else stacks.getValue(TopLevel.DASHBOARD) + current

    /** The navigation-rail item to highlight, or null on the More screen (it has no rail item). */
    val railSelection: Destination? get() = current.last().takeIf { it in RailDestinations }

    /** Bottom-bar tap: switch to [tab], or reset it to its root if it is already selected. */
    fun select(tab: TopLevel): NavState = if (tab == selected) {
        copy(stacks = stacks + (tab to listOf(tab.root)))
    } else {
        copy(selected = tab)
    }

    /**
     * Rail tap, Dashboard shortcut or More-screen item: show [destination].
     * Opening what is already shown changes nothing. This also covers "push":
     * in Phase 2 the only destinations below a tab root are Device and Tools.
     */
    fun open(destination: Destination): NavState {
        val tab = destination.tab
        return when {
            destination == tab.root -> when {
                tab != selected -> copy(selected = tab)
                current.size == 1 -> this
                else -> copy(stacks = stacks + (tab to listOf(tab.root)))
            }

            tab == selected && current.last() == destination -> this

            else -> copy(selected = tab, stacks = stacks + (tab to listOf(tab.root, destination)))
        }
    }

    /** Back: pop the current tab, else return to Dashboard, else null (leave the app). */
    fun back(): NavState? = when {
        current.size > 1 -> copy(stacks = stacks + (selected to current.dropLast(1)))
        selected != TopLevel.DASHBOARD -> copy(selected = TopLevel.DASHBOARD)
        else -> null
    }

    companion object {
        val Start: NavState = NavState(TopLevel.DASHBOARD, TopLevel.entries.associateWith { listOf(it.root) })

        /** Bottom bar on phones (approved decision P2-D1). */
        val BarDestinations: List<Destination> = TopLevel.entries.map { it.root }

        /** Navigation rail on wide windows: all six areas, no "More". */
        val RailDestinations: List<Destination> = listOf(
            Destination.Dashboard,
            Destination.Files,
            Destination.Notes,
            Destination.Device,
            Destination.Tools,
            Destination.Ai
        )
    }
}
