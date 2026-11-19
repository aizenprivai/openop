package io.github.aizenprivai.openop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import io.github.aizenprivai.openop.core.CoreBridge
import io.github.aizenprivai.openop.core.CoreInfo
import io.github.aizenprivai.openop.designsystem.OpenopTheme
import io.github.aizenprivai.openop.log.AppLog
import io.github.aizenprivai.openop.shell.OpenopApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenopTheme {
                OpenopApp(coreStatus = coreStatusText())
            }
        }
    }
}

/** What the Rust core reports, for the Dashboard footer. */
private sealed interface CoreStatus {
    data object Loading : CoreStatus

    data class Ready(val info: CoreInfo) : CoreStatus

    data object Failed : CoreStatus
}

@Composable
private fun coreStatusText(): String {
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
    return when (val s = status) {
        CoreStatus.Loading -> stringResource(R.string.core_status_loading)
        is CoreStatus.Ready -> stringResource(R.string.core_status_ok, s.info.coreVersion, s.info.buildProfile)
        CoreStatus.Failed -> stringResource(R.string.core_status_error)
    }
}
