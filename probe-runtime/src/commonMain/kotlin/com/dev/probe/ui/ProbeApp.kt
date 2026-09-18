package com.dev.probe.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.dev.probe.Probe
import com.dev.probe.ProbeScreen
import com.dev.probe.devactions.ClearDataResult
import com.dev.probe.devactions.PermissionDevPanel
import com.dev.probe.devactions.clearAppData
import com.dev.probe.devactions.clearAppDataDisclaimer
import com.dev.probe.devactions.resetProbeRuntimeState
import com.dev.probe.internal.ProbePlatformHolder
import com.dev.probe.internal.ProbeServices
import com.dev.probe.theme.ProbeTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ProbeApp(services: ProbeServices) {
    ProbeTheme(override = services.config.themeOverride) {
        SideEffect {
            if (!Probe.isEnabled) {
                Probe.install(plugins = services.plugins)
            }
        }

        val screen by Probe.screen.collectAsState()
        val hubVisible = remember { mutableStateOf(false) }
        val modeVisible = remember { mutableStateOf(false) }
        val permissionsVisible = remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        var confirmClearAppData by remember { mutableStateOf(false) }
        var clearAppDataResult by remember { mutableStateOf<ClearDataResult?>(null) }

        LaunchedEffect(screen) {
            hubVisible.value = screen == ProbeScreen.HUB
            modeVisible.value = screen == ProbeScreen.NETWORK_MODE
            permissionsVisible.value = screen == ProbeScreen.PERMISSIONS
        }

        BackHandler(enabled = screen != null) {
            Probe.navigateBack()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            ProbeHubSheet(
                plugins = Probe.plugins,
                isVisible = hubVisible,
                onDismiss = { Probe.dismissAll() },
                onPluginClick = { plugin ->
                    // "network" keeps its own in-app/browser mode picker; every other plugin
                    // opens straight into its full-screen inspector.
                    if (plugin.id == "network") Probe.showNetworkModePicker() else Probe.showPluginInspector(plugin.id)
                },
                onPermissionsClick = { Probe.showPermissions() },
                onAppDataClick = { confirmClearAppData = true },
            )

            NetworkOutputModeSheet(
                isVisible = modeVisible,
                onBack = { Probe.showHub() },
                onOpenInspector = { Probe.showInspector() },
                outputController = services.outputController,
                browserController = services.browserController,
                preferencesStore = services.preferencesStore,
            )

            PermissionDevPanel(
                isVisible = permissionsVisible,
                onBack = { Probe.showHub() },
            )

            ProbeInspectorPanel()
        }

        if (confirmClearAppData) {
            AlertDialog(
                onDismissRequest = { confirmClearAppData = false },
                title = { Text("Clear app data?") },
                text = { Text(clearAppDataDisclaimer) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmClearAppData = false
                            scope.launch {
                                resetProbeRuntimeState(
                                    sessionManager = services.sessionManager,
                                    browserController = services.browserController,
                                    notifierBridge = services.notifierBridge,
                                    crashLogStore = services.crashLogStore,
                                )
                                clearAppDataResult = clearAppData(ProbePlatformHolder.requirePlatform())
                            }
                        },
                    ) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClearAppData = false }) {
                        Text("Cancel")
                    }
                },
            )
        }

        clearAppDataResult?.let { result ->
            ClearAppDataResultDialog(result = result, onDismiss = { clearAppDataResult = null })
        }
    }
}

@Composable
private fun ClearAppDataResultDialog(result: ClearDataResult, onDismiss: () -> Unit) {
    val message =
        when (result) {
            is ClearDataResult.FullResetTriggered -> "App data cleared. The app will restart shortly."
            is ClearDataResult.PartialClear ->
                if (result.clearedItems.isEmpty()) {
                    "Nothing to clear."
                } else {
                    "Cleared: ${result.clearedItems.joinToString(", ")}"
                }
            is ClearDataResult.Failed -> "Couldn't clear app data: ${result.reason}"
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App data") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
    )
}
