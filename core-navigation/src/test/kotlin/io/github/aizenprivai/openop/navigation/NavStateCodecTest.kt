package io.github.aizenprivai.openop.navigation

import io.github.aizenprivai.openop.navigation.Destination.Device
import io.github.aizenprivai.openop.navigation.Destination.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class NavStateCodecTest {
    @Test
    fun roundTripKeepsSelectedTabAndEveryStack() {
        val state = NavState.Start.open(Device).open(Files)
        assertEquals(state, NavStateCodec.decode(NavStateCodec.encode(state)))
    }

    @Test
    fun startStateRoundTrips() {
        assertEquals(NavState.Start, NavStateCodec.decode(NavStateCodec.encode(NavState.Start)))
    }

    @Test
    fun missingOrBlankInputFallsBackToStart() {
        assertEquals(NavState.Start, NavStateCodec.decode(null))
        assertEquals(NavState.Start, NavStateCodec.decode(""))
    }

    @Test
    fun corruptInputFallsBackToStart() {
        listOf(
            "garbage",
            "v1;selected=nowhere",
            "v1;selected=files;files=unknown",
            "v1;selected=more;more=device", // stack must start with the tab root
            "v1;selected=files;files=files,device", // destination from another tab
            "v1;selected=files;files=", // empty stack
            "v999;selected=files", // unknown format version
            "v1", // no selected tab
            "v1;selected=device", // not a tab
            "v1;selected=dashboard;dashboard=dashboard,files", // destination from another tab
            "v1;selected=more;more=more,device,device", // duplicate entry
            "v1;selected=more;more=more,more", // duplicate root
            "v1;selected=files;files=files,files" // duplicate root
        ).forEach { input ->
            assertEquals("input: $input", NavState.Start, NavStateCodec.decode(input))
        }
    }

    @Test
    fun missingTabsDefaultToTheirRoot() {
        val decoded = NavStateCodec.decode("v1;selected=more;more=more,device")
        assertEquals(TopLevel.MORE, decoded.selected)
        assertEquals(listOf(Destination.More, Device), decoded.current)
        assertEquals(listOf(Destination.Files), decoded.stacks[TopLevel.FILES])
    }

    @Test
    fun roundTripKeepsToolsUnderMore() {
        val state = NavState.Start.open(Destination.Tools)
        assertEquals(state, NavStateCodec.decode(NavStateCodec.encode(state)))
    }

    @Test
    fun trailingSeparatorIsTolerated() {
        assertEquals(TopLevel.FILES, NavStateCodec.decode("v1;selected=files;").selected)
    }
}
