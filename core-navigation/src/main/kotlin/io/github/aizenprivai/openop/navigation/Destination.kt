package io.github.aizenprivai.openop.navigation

/**
 * Every place the app shell can show. Ids are stable: they are written into
 * saved state (see [NavStateCodec]), so never change an existing id.
 */
sealed interface Destination {
    val id: String

    /** The top-level tab this destination lives in. */
    val tab: TopLevel

    data object Dashboard : Destination {
        override val id = "dashboard"
        override val tab get() = TopLevel.DASHBOARD
    }

    data object Files : Destination {
        override val id = "files"
        override val tab get() = TopLevel.FILES
    }

    data object Notes : Destination {
        override val id = "notes"
        override val tab get() = TopLevel.NOTES
    }

    data object Ai : Destination {
        override val id = "ai"
        override val tab get() = TopLevel.AI
    }

    /** The "More" screen listing Device and Tools (approved decision P2-D1). */
    data object More : Destination {
        override val id = "more"
        override val tab get() = TopLevel.MORE
    }

    data object Device : Destination {
        override val id = "device"
        override val tab get() = TopLevel.MORE
    }

    data object Tools : Destination {
        override val id = "tools"
        override val tab get() = TopLevel.MORE
    }

    companion object {
        val all: List<Destination> get() = listOf(Dashboard, Files, Notes, Ai, More, Device, Tools)

        fun fromId(id: String): Destination? = all.firstOrNull { it.id == id }
    }
}

/** The five tabs of the bottom bar. Each keeps its own back stack. */
enum class TopLevel {
    DASHBOARD,
    FILES,
    NOTES,
    AI,
    MORE;

    /** The destination at the bottom of this tab's back stack. */
    val root: Destination
        get() = when (this) {
            DASHBOARD -> Destination.Dashboard
            FILES -> Destination.Files
            NOTES -> Destination.Notes
            AI -> Destination.Ai
            MORE -> Destination.More
        }
}
