package io.github.aizenprivai.openop.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestinationTest {
    @Test
    fun idsAreUniqueAndResolveBack() {
        val ids = Destination.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        Destination.all.forEach { assertEquals(it, Destination.fromId(it.id)) }
        assertNull(Destination.fromId("nope"))
    }

    @Test
    fun deviceAndToolsBelongToMore() {
        assertEquals(TopLevel.MORE, Destination.Device.tab)
        assertEquals(TopLevel.MORE, Destination.Tools.tab)
        assertEquals(TopLevel.FILES, Destination.Files.tab)
    }
}
