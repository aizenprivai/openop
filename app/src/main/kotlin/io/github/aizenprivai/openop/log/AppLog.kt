package io.github.aizenprivai.openop.log

import android.util.Log
import io.github.aizenprivai.openop.BuildConfig

/**
 * The one logging entry point for OPENOP.
 *
 * Rules (CLAUDE.md §7.3):
 * - Never log user content: note text, conversation text, file contents,
 *   or file paths that could identify the user.
 * - Debug and info messages are logged in debug builds only.
 *   Release builds log warnings and errors only.
 *
 * Tags are "OPENOP/<area>", e.g. "OPENOP/core".
 */
object AppLog {
    private const val PREFIX = "OPENOP/"

    fun d(area: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag(area), message)
    }

    fun i(area: String, message: String) {
        if (BuildConfig.DEBUG) Log.i(tag(area), message)
    }

    fun w(area: String, message: String, error: Throwable? = null) {
        Log.w(tag(area), message, error)
    }

    fun e(area: String, message: String, error: Throwable? = null) {
        Log.e(tag(area), message, error)
    }

    private fun tag(area: String) = PREFIX + area
}
