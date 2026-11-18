package io.github.aizenprivai.openop.core

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Runs on the host JVM, where the Android native library cannot load.
 * Every call must return a handled failure instead of throwing, including
 * repeated calls, which see NoClassDefFoundError after a failed class init.
 */
class CoreBridgeLoadFailureTest {
    @Test
    fun missingNativeLibraryIsAHandledFailureEveryTime() {
        repeat(3) { attempt ->
            val result = CoreBridge.coreInfo()
            val error = result.exceptionOrNull()
            assertTrue("attempt $attempt: expected failure, got $result", result.isFailure)
            assertTrue("attempt $attempt: expected Internal, got $error", error is CoreBridgeException.Internal)
            assertTrue(
                "attempt $attempt: expected a LinkageError cause, got ${error?.cause}",
                error?.cause is LinkageError
            )
        }
    }
}
