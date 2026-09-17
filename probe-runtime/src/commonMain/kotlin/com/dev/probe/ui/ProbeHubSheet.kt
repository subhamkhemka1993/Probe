package com.dev.probe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dev.probe.plugin.ProbePlugin
import com.dev.probe.preview.ProbePreviewData
import com.dev.probe.preview.ProbeSheetPreviewContainer
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.ui.primitives.ProbeBottomSheet
import com.dev.probe.ui.primitives.ProbeDivider
import com.dev.probe.ui.primitives.ProbeListRow

private fun hubPluginIcon(pluginId: String): ImageVector = when (pluginId) {
    "network" -> Icons.Filled.SwapHoriz
    else -> Icons.Filled.Info
}

@Composable
private fun HubSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = LocalProbeTypography.current.labelSmall,
        color = LocalProbeColors.current.textSecondary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
internal fun ProbeHubSheet(
    plugins: List<ProbePlugin>,
    isVisible: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onPluginClick: (ProbePlugin) -> Unit,
    onPermissionsClick: () -> Unit,
    onAppDataClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProbeBottomSheet(
        isVisible = isVisible,
        title = "Debug Tools",
        onDismissRequest = onDismiss,
        modifier = modifier,
        allowDismiss = true,
        headerIcon = Icons.Filled.Summarize,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Column {
            if (plugins.isNotEmpty()) {
                HubSectionHeader(title = "Plugins")
            }
            plugins.forEachIndexed { index, plugin ->
                ProbeListRow(
                    onClick = { onPluginClick(plugin) },
                    leftContent = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = hubPluginIcon(plugin.id),
                                contentDescription = null,
                                tint = LocalProbeColors.current.primary,
                            )
                            ProbeListLabel(
                                title = plugin.displayName,
                                subtitle = plugin.description,
                            )
                        }
                    },
                    rightContent = {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = LocalProbeColors.current.textSecondary,
                        )
                    },
                )
                if (index < plugins.lastIndex) {
                    ProbeDivider()
                }
            }
            if (plugins.isNotEmpty()) {
                ProbeDivider()
            }
            HubSectionHeader(title = "Dev")
            DevActionsHubSection(
                onPermissionsClick = onPermissionsClick,
                onAppDataClick = onAppDataClick,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun ProbeHubSheetPreview() {
    val visible = remember { mutableStateOf(true) }
    ProbeSheetPreviewContainer {
        ProbeHubSheet(
            plugins = ProbePreviewData.plugins,
            isVisible = visible,
            onDismiss = {},
            onPluginClick = {},
            onPermissionsClick = {},
            onAppDataClick = {},
        )
    }
}
