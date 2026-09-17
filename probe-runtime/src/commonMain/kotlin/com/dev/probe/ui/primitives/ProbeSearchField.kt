package com.dev.probe.ui.primitives

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography

@Composable
internal fun ProbeSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = typography.bodyMedium,
        placeholder = {
            Text(
                text = placeholder,
                style = typography.bodyMedium,
                color = colors.textSecondary,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = colors.textSecondary,
            )
        },
        trailingIcon = {
            AnimatedVisibility(visible = value.isNotEmpty()) {
                ProbeIconButton(
                    onClick = { onValueChange("") },
                    icon = Icons.Filled.Close,
                    tint = colors.textSecondary,
                    contentDescription = "Clear search",
                )
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors =
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.divider,
            cursorColor = colors.primary,
        ),
    )
}

@ThemePreviews
@Composable
private fun ProbeSearchFieldPreview() {
    ProbeBackgroundPreviewContainer {
        var query by remember { mutableStateOf("GET /api") }
        Column(modifier = Modifier.padding(8.dp)) {
            ProbeSearchField(
                value = query,
                onValueChange = { query = it },
                onDone = {},
                placeholder = "Search URL, method, status…",
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ProbeSearchFieldEmptyPreview() {
    ProbeBackgroundPreviewContainer {
        ProbeSearchField(
            value = "",
            onValueChange = {},
            onDone = {},
            modifier = Modifier.padding(8.dp),
        )
    }
}
