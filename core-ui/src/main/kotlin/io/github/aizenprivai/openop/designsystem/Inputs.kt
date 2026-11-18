package io.github.aizenprivai.openop.designsystem

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics

/**
 * A text field with a visible [label], optional [supportingText], and an
 * [errorText] that replaces the supporting text and is announced as an error.
 */
@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    errorText: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true
) {
    val message = errorText ?: supportingText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = if (errorText != null) modifier.semantics { error(errorText) } else modifier,
        supportingText = message?.let { { Text(it) } },
        isError = errorText != null,
        singleLine = singleLine,
        enabled = enabled
    )
}
