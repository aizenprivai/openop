package io.github.aizenprivai.openop.navigation

/**
 * Text form of [NavState] for saved instance state, e.g.
 * `v1;selected=more;dashboard=dashboard;more=more,device`.
 *
 * Decoding never throws: anything missing, unknown or invalid gives
 * [NavState.Start]. Tabs not listed start at their root.
 */
object NavStateCodec {
    private const val VERSION = "v1"

    fun encode(state: NavState): String = buildList {
        add(VERSION)
        add("selected=${state.selected.root.id}")
        TopLevel.entries.forEach { tab ->
            add("${tab.root.id}=${state.stacks.getValue(tab).joinToString(",") { it.id }}")
        }
    }.joinToString(";")

    fun decode(text: String?): NavState {
        if (text.isNullOrBlank()) return NavState.Start
        return runCatching { parse(text) }.getOrNull() ?: NavState.Start
    }

    private fun parse(text: String): NavState? {
        val parts = text.split(";").filter { it.isNotEmpty() }
        if (parts.firstOrNull() != VERSION) return null
        val fields = parts.drop(1).associate { part ->
            val (key, value) = part.split("=", limit = 2).takeIf { it.size == 2 } ?: return null
            key to value
        }
        val selected = tabFor(fields["selected"] ?: return null) ?: return null
        val stacks = TopLevel.entries.associateWith { tab ->
            val ids = fields[tab.root.id] ?: return@associateWith listOf(tab.root)
            ids.split(",").map { Destination.fromId(it) ?: return null }
        }
        // The NavState constructor validates the stacks; invalid ones throw and decode to Start.
        return NavState(selected, stacks)
    }

    private fun tabFor(id: String): TopLevel? = TopLevel.entries.firstOrNull { it.root.id == id }
}
