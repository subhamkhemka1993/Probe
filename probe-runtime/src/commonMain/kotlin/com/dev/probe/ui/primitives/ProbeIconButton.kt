package com.dev.probe.ui.primitives

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.theme.LocalProbeColors

@Composable
internal fun ProbeIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
) {
    val colors = LocalProbeColors.current
    IconButton(
        modifier = modifier.size(size),
        onClick = onClick,
        enabled = enabled,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else colors.textSecondary,
            modifier = Modifier.size(size * 0.7f),
        )
    }
}

@ThemePreviews
@Composable
private fun ProbeIconButtonPreview() {
    ProbeBackgroundPreviewContainer {
        Box(modifier = Modifier.fillMaxWidth()) {
            ProbeIconButton(
                onClick = {},
                icon = Icons.Filled.Close,
                tint = LocalProbeColors.current.textPrimary,
                contentDescription = "Close",
            )
        }
    }
}
