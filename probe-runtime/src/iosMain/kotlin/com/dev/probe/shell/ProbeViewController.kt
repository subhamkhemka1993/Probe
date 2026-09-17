package com.dev.probe.shell

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.ComposeUIViewController
import com.dev.probe.Probe
import com.dev.probe.api.ProbeRuntime
import com.dev.probe.api.ProbeStartScreen
import com.dev.probe.ui.ProbeApp
import platform.UIKit.UIViewController

/**
 * Platform shell hosting [ProbeApp] on iOS. Created exclusively via
 * `ProbeViewControllerPresenter`, which owns presenting/dismissing this controller.
 */
internal fun createProbeViewController(screen: ProbeStartScreen): UIViewController = ComposeUIViewController {
    val services = ProbeRuntime.services()
    LaunchedEffect(screen) {
        when (screen) {
            ProbeStartScreen.Hub -> Probe.showHub()
            ProbeStartScreen.Inspector -> Probe.showInspector()
        }
    }
    ProbeApp(services = services)
}
