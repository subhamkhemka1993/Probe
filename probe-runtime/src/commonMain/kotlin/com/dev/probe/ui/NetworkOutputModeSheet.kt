package com.dev.probe.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.dev.probe.NetworkOutputMode
import com.dev.probe.browser.NetworkBrowserController
import com.dev.probe.policy.NetworkOutputController
import com.dev.probe.prefs.DebugPreferences
import com.dev.probe.prefs.DebugPreferencesStore
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.ui.primitives.ProbeBottomSheet
import com.dev.probe.ui.primitives.ProbeDivider
import com.dev.probe.ui.primitives.ProbePrimaryButton
import com.dev.probe.ui.primitives.ProbeRadioButton
import kotlinx.coroutines.launch

private data class NetworkModeOption(val mode: NetworkOutputMode, val label: String, val subtitle: String)

private val networkModeOptions =
    listOf(
        NetworkModeOption(
            mode = NetworkOutputMode.INSPECTOR,
            label = "In-app inspector",
            subtitle = "Capture traffic with list and detail views",
        ),
        NetworkModeOption(
            mode = NetworkOutputMode.BROWSER,
            label = "Browser inspector",
            subtitle = "Inspect traffic in a desktop browser",
        ),
    )

@Composable
internal fun NetworkOutputModeSheet(
    isVisible: MutableState<Boolean>,
    onBack: () -> Unit,
    onOpenInspector: () -> Unit,
    outputController: NetworkOutputController,
    browserController: NetworkBrowserController,
    preferencesStore: DebugPreferencesStore,
    modifier: Modifier = Modifier,
) {
    val preferences by preferencesStore.preferences.collectAsState(initial = DebugPreferences())
    val selectedMode = preferences.networkOutputMode
    val connectionInfo by browserController.connectionInfo.collectAsState()
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    ProbeBottomSheet(
        isVisible = isVisible,
        title = "Network Output",
        onDismissRequest = onBack,
        modifier = modifier,
        allowDismiss = true,
        allowBack = true,
        onBackPress = onBack,
        headerIcon = Icons.Filled.SwapHoriz,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        footer = {
            when (selectedMode) {
                NetworkOutputMode.INSPECTOR -> {
                    ProbePrimaryButton(
                        title = "Open Inspector",
                        onClick = onOpenInspector,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                NetworkOutputMode.BROWSER -> {
                    BrowserConnectionCard(
                        connectionInfo = connectionInfo,
                        onCopyLink = { clipboard.setText(AnnotatedString(it)) },
                        onCopyToken = { clipboard.setText(AnnotatedString(it)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) {
        NetworkOutputModeSheetContent(
            selectedMode = selectedMode,
            onModeSelected = { mode ->
                scope.launch {
                    outputController.applyMode(mode)
                }
            },
        )
    }
}

@Composable
internal fun NetworkOutputModeSheetContent(selectedMode: NetworkOutputMode, onModeSelected: (NetworkOutputMode) -> Unit) {
    Column {
        networkModeOptions.forEachIndexed { index, option ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ProbeRadioButton(
                    modifier = Modifier.weight(1f),
                    onChecked = {
                        onModeSelected(option.mode)
                    },
                    checked = selectedMode == option.mode,
                    content = {
                        ProbeListLabel(
                            title = option.label,
                            subtitle = option.subtitle,
                        )
                    },
                )
            }
            if (index < networkModeOptions.lastIndex) {
                ProbeDivider()
            }
        }
    }
}

@ThemePreviews
@Composable
private fun NetworkOutputModeSheetPreview() {
    ProbeBackgroundPreviewContainer {
        NetworkOutputModeSheetContent(
            selectedMode = NetworkOutputMode.INSPECTOR,
            onModeSelected = {},
        )
    }
}
