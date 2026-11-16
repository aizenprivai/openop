package io.github.aizenprivai.openop.core

import io.github.aizenprivai.openop.core.ffi.CoreException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Runs on the host JVM: checks error mapping without loading the native library. */
class CoreBridgeMappingTest {
    @Test
    fun invalidInputKeepsReasonAndCause() {
        val ffiError = CoreException.InvalidInput("input is empty")

        val mapped = CoreBridge.mapCoreException(ffiError)

        assertTrue(mapped is CoreBridgeException.InvalidInput)
        assertEquals("input is empty", (mapped as CoreBridgeException.InvalidInput).reason)
        assertSame(ffiError, mapped.cause)
    }
}
