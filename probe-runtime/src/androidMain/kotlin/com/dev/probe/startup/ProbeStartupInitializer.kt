package com.dev.probe.startup

import android.content.Context
import androidx.startup.Initializer
import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbeInstaller
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.api.ProbeRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Registers the real install [ProbeInstaller.Hook] as early as possible (via App Startup's
 * `ContentProvider`, which runs before `Application.onCreate()`). Deliberately does NOT call
 * [ProbeRuntime.initialize] itself — a host app's own build/flag config typically isn't valid
 * yet at this point in process startup. The actual call happens later, once the host calls
 * [ProbeInstaller.install]; this class only makes that later call reach `:probe-runtime` at all.
 */
class ProbeStartupInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        ProbeInstaller.register(
            object : ProbeInstaller.Hook {
                override fun install(
                    config: ProbeConfig,
                    platform: ProbePlatformContext,
                ) {
                    ProbeRuntime.initialize(
                        config = config,
                        platform = platform,
                        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
                    )
                }
            },
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
