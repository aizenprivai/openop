package io.github.aizenprivai.openop.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on a device: loads the real Rust library through UniFFI. */
@RunWith(AndroidJUnit4::class)
class CoreBridgeInstrumentedTest {
    @Test
    fun coreInfoReturnsVersionFromRustWorkspace() {
        val info = CoreBridge.coreInfo().getOrThrow()

        assertEquals(BuildConfig.EXPECTED_CORE_VERSION, info.coreVersion)
        assertTrue(info.buildProfile == "debug" || info.buildProfile == "release")
    }

    @Test
    fun echoValidatedReturnsInput() {
        assertEquals("abc", CoreBridge.echoValidated("abc").getOrThrow())
    }

    @Test
    fun emptyInputBecomesHandledError() {
        val error = CoreBridge.echoValidated("").exceptionOrNull()

        assertTrue("expected InvalidInput, got $error", error is CoreBridgeException.InvalidInput)
        assertEquals("input is empty", (error as CoreBridgeException.InvalidInput).reason)
    }
}
