package io.github.aizenprivai.openop.designsystem

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.aizenprivai.openop.navigation.Destination

/** How a destination looks in navigation and placeholders. */
data class DestinationVisuals(
    @StringRes val label: Int,
    @StringRes val purpose: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int,
    /** Roadmap phase that implements this area (CLAUDE.md §10), or null for shell screens. */
    val plannedPhase: Int?
)

val Destination.visuals: DestinationVisuals
    get() = when (this) {
        Destination.Dashboard -> DestinationVisuals(
            R.string.openop_dest_dashboard,
            R.string.openop_purpose_dashboard,
            R.drawable.openop_ic_space_dashboard,
            R.drawable.openop_ic_space_dashboard_filled,
            plannedPhase = 4
        )

        Destination.Files -> DestinationVisuals(
            R.string.openop_dest_files,
            R.string.openop_purpose_files,
            R.drawable.openop_ic_folder,
            R.drawable.openop_ic_folder_filled,
            plannedPhase = 5
        )

        Destination.Notes -> DestinationVisuals(
            R.string.openop_dest_notes,
            R.string.openop_purpose_notes,
            R.drawable.openop_ic_sticky_note_2,
            R.drawable.openop_ic_sticky_note_2_filled,
            plannedPhase = 6
        )

        Destination.Device -> DestinationVisuals(
            R.string.openop_dest_device,
            R.string.openop_purpose_device,
            R.drawable.openop_ic_smartphone,
            R.drawable.openop_ic_smartphone_filled,
            plannedPhase = 4
        )

        Destination.Tools -> DestinationVisuals(
            R.string.openop_dest_tools,
            R.string.openop_purpose_tools,
            R.drawable.openop_ic_build,
            R.drawable.openop_ic_build_filled,
            plannedPhase = 8
        )

        Destination.Ai -> DestinationVisuals(
            R.string.openop_dest_ai,
            R.string.openop_purpose_ai,
            R.drawable.openop_ic_smart_toy,
            R.drawable.openop_ic_smart_toy_filled,
            plannedPhase = 7
        )

        Destination.More -> DestinationVisuals(
            R.string.openop_dest_more,
            R.string.openop_purpose_more,
            R.drawable.openop_ic_more_horiz,
            R.drawable.openop_ic_more_horiz,
            plannedPhase = null
        )
    }
