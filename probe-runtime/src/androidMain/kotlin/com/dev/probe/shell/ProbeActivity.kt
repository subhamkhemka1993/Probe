package com.dev.probe.shell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
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
    /**
     * Mirrors the iOS presenter: once the user has seen a non-null [Probe.screen], a later null
     * means they dismissed the hub/inspector from within the shell, so this activity should
     * finish rather than leave a blank window behind. The `sawScreen` flag tracked inside guards
     * against finishing on the initial null value, before the `LaunchedEffect(screen)` effect
     * has had a chance to route to Hub/Inspector.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ProbeRuntime.isEnabled()) {
            finish()
            return
        }
        onBackPressedDispatcher.addCallback(this) {
            if (!Probe.navigateBack()) finish()
        }
        val screen =
            intent
                .getStringExtra(PROBE_START_SCREEN)
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
}
