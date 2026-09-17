package com.dev.probe.shell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import com.dev.probe.Probe
import com.dev.probe.api.PROBE_START_SCREEN
import com.dev.probe.api.ProbeRuntime
import com.dev.probe.api.ProbeStartScreen
import com.dev.probe.ui.ProbeApp

/**
 * Platform shell hosting [ProbeApp] on Android. Normally launched via
 * [com.dev.probe.api.ProbeLauncher]; also reachable directly from the home screen via the
 * `ProbeLauncherAlias` `activity-alias` (a static manifest entry, always enabled in debug
 * builds). [ProbeConfig.isEnabled][com.dev.probe.api.ProbeConfig.isEnabled] is the only
 * runtime gate for that second path — checked here rather than in the manifest, since
 * `android:enabled` can't evaluate a runtime predicate.
 *
 * Must stay public (not `internal`) so the merged manifest can resolve `.shell.ProbeActivity`.
 */
class ProbeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ProbeRuntime.isEnabled()) {
            finish()
            return
        }
        val screen = intent.getStringExtra(PROBE_START_SCREEN)
            ?.let { runCatching { ProbeStartScreen.valueOf(it) }.getOrNull() }
            ?: ProbeStartScreen.Hub

        setContent {
            val services = ProbeRuntime.services()
            LaunchedEffect(screen) {
                when (screen) {
                    ProbeStartScreen.Hub -> Probe.showHub()
                    ProbeStartScreen.Inspector -> Probe.showInspector()
                }
            }
            // Mirrors the iOS presenter: once the user has seen a non-null screen, a later
            // null means they dismissed the hub/inspector from within the shell, so this
            // activity should finish rather than leave a blank window behind. `sawScreen`
            // guards against finishing on the initial null value, before the effect above
            // has had a chance to route to Hub/Inspector.
            LaunchedEffect(Unit) {
                var sawScreen = false
                Probe.screen.collect { current ->
                    if (current != null) {
                        sawScreen = true
                    } else if (sawScreen) {
                        finish()
                    }
                }
            }
            ProbeApp(services = services)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!Probe.navigateBack()) finish()
    }
}
