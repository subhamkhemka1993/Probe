package com.dev.probe.ui.primitives

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.theme.semiBold

@Composable
internal fun ProbeRadioButton(
    modifier: Modifier = Modifier,
    onChecked: () -> Unit,
    checked: Boolean = false,
    readOnly: Boolean = false,
    content: @Composable RowScope.() -> Unit = {},
    innerPadding: PaddingValues = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
) {
    val colors = LocalProbeColors.current
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .then(
                if (!readOnly) {
                    Modifier.clickable(onClick = onChecked)
                } else {
                    Modifier
                },
            ).padding(innerPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(
            selected = checked,
            onClick = if (!readOnly) onChecked else null,
            enabled = !readOnly,
            colors =
            RadioButtonDefaults.colors(
                selectedColor = colors.primary,
                unselectedColor = colors.textSecondary,
            ),
        )
        Row(
            modifier = Modifier.weight(1f),
            content = content,
        )
    }
}

@ThemePreviews
@Composable
private fun ProbeRadioButtonPreview() {
    ProbeBackgroundPreviewContainer {
        Column {
            ProbeRadioButton(
                onChecked = {},
                checked = true,
                content = {
                    Column {
                        Text(
                            text = "In-app inspector",
                            style = LocalProbeTypography.current.bodyMedium.semiBold(),
                            color = LocalProbeColors.current.textPrimary,
                        )
                        Text(
                            text = "Capture traffic with list and detail views",
                            style = LocalProbeTypography.current.labelMedium,
                            color = LocalProbeColors.current.textSecondary,
                        )
                    }
                },
            )
            ProbeRadioButton(
                onChecked = {},
                checked = false,
                content = {
                    Column {
                        Text(
                            text = "In-app inspector",
                            style = LocalProbeTypography.current.bodyMedium.semiBold(),
                            color = LocalProbeColors.current.textPrimary,
                        )
                        Text(
                            text = "Capture traffic with list and detail views",
                            style = LocalProbeTypography.current.labelMedium,
                            color = LocalProbeColors.current.textSecondary,
                        )
                    }
                },
            )
        }
    }
}
