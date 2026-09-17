package com.dev.probe.ui.primitives

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography

@Composable
internal fun ProbePrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minHeight: Dp = 48.dp,
) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
        modifier
            .heightIn(min = minHeight),
        colors =
        ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.primary.copy(alpha = 0.4f),
            disabledContentColor = colors.onPrimary.copy(alpha = 0.6f),
        ),
    ) {
        Text(
            text = title,
            style = typography.labelLarge,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@ThemePreviews
@Composable
private fun ProbePrimaryButtonPreview() {
    ProbeBackgroundPreviewContainer {
        ProbePrimaryButton(
            title = "Open Inspector",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
