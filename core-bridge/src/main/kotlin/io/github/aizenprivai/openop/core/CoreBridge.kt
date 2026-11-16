package io.github.aizenprivai.openop.core

import io.github.aizenprivai.openop.core.ffi.CoreException
import io.github.aizenprivai.openop.core.ffi.coreInfo as ffiCoreInfo
import io.github.aizenprivai.openop.core.ffi.echoValidated as ffiEchoValidated

/** Basic information about the Rust core. */
data class CoreInfo(val coreVersion: String, val buildProfile: String)

/** Errors reported by [CoreBridge]. Callers never see generated UniFFI types. */
sealed class CoreBridgeException(message: String, cause: Throwable?) : Exception(message, cause) {
    /** The core rejected the input. [reason] is safe to show to the user. */
    class InvalidInput(val reason: String, cause: Throwable? = null) :
        CoreBridgeException("Invalid input: $reason", cause)

    /** The native library failed: not loaded, or a Rust panic. This is a bug. */
    class Internal(cause: Throwable) : CoreBridgeException("Rust core failure", cause)
}

/**
 * The only entry point from Kotlin into the Rust core.
 *
 * Every call returns a [Result]; failures are always [CoreBridgeException].
 * Calls may block, so call them off the main thread.
 */
object CoreBridge {
    fun coreInfo(): Result<CoreInfo> = call {
        val info = ffiCoreInfo()
        CoreInfo(coreVersion = info.coreVersion, buildProfile = info.buildProfile)
    }

    /** Returns [input] unchanged, or [CoreBridgeException.InvalidInput] if it is blank. */
    fun echoValidated(input: String): Result<String> = call { ffiEchoValidated(input) }

    private inline fun <T> call(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CoreException) {
        Result.failure(mapCoreException(e))
    } catch (e: Exception) {
        // Includes UniFFI's InternalException (Rust panic).
        Result.failure(CoreBridgeException.Internal(e))
    } catch (e: UnsatisfiedLinkError) {
        // Native library missing or for the wrong ABI.
        Result.failure(CoreBridgeException.Internal(e))
    }

    internal fun mapCoreException(e: CoreException): CoreBridgeException = when (e) {
        is CoreException.InvalidInput -> CoreBridgeException.InvalidInput(e.reason, e)
    }
}
