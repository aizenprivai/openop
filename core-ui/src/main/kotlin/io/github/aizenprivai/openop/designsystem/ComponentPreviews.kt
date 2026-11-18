package io.github.aizenprivai.openop.designsystem

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ComponentsPreview() {
    OpenopTheme {
        Surface {
            Column(modifier = Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                SectionHeader("Section")
                InfoCard(title = "Info card", body = "Supporting text", onClick = {})
                ListRow(headline = "List row", supporting = "Supporting text", onClick = {})
                PrimaryButton("Primary", onClick = {})
                SecondaryButton("Secondary", onClick = {})
                DestructiveButton("Delete", onClick = {})
                LabeledTextField(value = "", onValueChange = {}, label = "Label", errorText = "Error text")
            }
        }
    }
}

@Preview(name = "States", showBackground = true, heightDp = 640)
@Composable
private fun StatesPreview() {
    OpenopTheme {
        Surface {
            Column {
                EmptyState(title = "Empty", body = "Nothing here yet.", modifier = Modifier.weight(1f))
                ErrorState(message = "Something went wrong.", onRetry = {}, modifier = Modifier.weight(1f))
                LoadingState(label = "Loading", modifier = Modifier.weight(1f))
            }
        }
    }
}
