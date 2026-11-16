package io.github.aizenprivai.openop.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.aizenprivai.openop.R
import io.github.aizenprivai.openop.core.CoreBridge
import io.github.aizenprivai.openop.core.CoreInfo
import io.github.aizenprivai.openop.log.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What the placeholder shows about the Rust core. */
sealed interface CoreStatus {
    data object Loading : CoreStatus

    data class Ready(val info: CoreInfo) : CoreStatus

    data object Failed : CoreStatus
}

/**
 * Phase 1 developer placeholder. Says plainly that nothing is implemented,
 * and shows the result of one call into the Rust core.
 * The real app shell and theme arrive in Phase 2.
 */
@Composable
fun PlaceholderScreen() {
    val status by produceState<CoreStatus>(initialValue = CoreStatus.Loading) {
        // Native calls stay off the main thread.
        value = withContext(Dispatchers.Default) {
            CoreBridge.coreInfo().fold(
                onSuccess = { CoreStatus.Ready(it) },
                onFailure = { error ->
                    AppLog.e("core", "Rust core call failed", error)
                    CoreStatus.Failed
                }
            )
        }
    }
    PlaceholderContent(status)
}

@Composable
fun PlaceholderContent(status: CoreStatus) {
    // minSdk is 31, so Dynamic Color is always available.
    val context = LocalContext.current
    val colors =
        if (isSystemInDarkTheme()) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }

    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .safeDrawingPadding()
                        .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.placeholder_title),
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = stringResource(R.string.placeholder_body),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = coreStatusText(status),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun coreStatusText(status: CoreStatus): String = when (status) {
    CoreStatus.Loading -> stringResource(R.string.core_status_loading)
    is CoreStatus.Ready -> stringResource(R.string.core_status_ok, status.info.coreVersion, status.info.buildProfile)
    CoreStatus.Failed -> stringResource(R.string.core_status_error)
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderContentPreview() {
    PlaceholderContent(CoreStatus.Ready(CoreInfo(coreVersion = "0.1.0", buildProfile = "debug")))
}
