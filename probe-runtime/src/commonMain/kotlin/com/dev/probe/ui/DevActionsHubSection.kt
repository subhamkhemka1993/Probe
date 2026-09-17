package com.dev.probe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.ui.primitives.ProbeDivider
import com.dev.probe.ui.primitives.ProbeListRow

/**
 * Hub "Dev" section rows — Permissions (2e) and App data (wired in 2f).
 */
@Composable
internal fun DevActionsHubSection(onPermissionsClick: () -> Unit, onAppDataClick: () -> Unit) {
    val colors = LocalProbeColors.current

    ProbeListRow(
        onClick = onPermissionsClick,
        leftContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = colors.primary)
                ProbeListLabel(title = "Permissions", subtitle = "Check and request host app permissions")
            }
        },
        rightContent = {
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textSecondary)
        },
    )
    ProbeDivider()
    ProbeListRow(
        onClick = onAppDataClick,
        leftContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Filled.DeleteSweep, contentDescription = null, tint = colors.primary)
                ProbeListLabel(title = "App data", subtitle = "Clear app storage and restart")
            }
        },
        rightContent = {
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textSecondary)
        },
    )
}
