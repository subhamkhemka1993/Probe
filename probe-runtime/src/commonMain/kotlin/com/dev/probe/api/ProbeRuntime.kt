package com.dev.probe.api

import com.dev.probe.internal.PROBE_SELF_DATABASE_NAME
import com.dev.probe.internal.ProbeGraphFactory
import com.dev.probe.internal.ProbePlatformHolder
import com.dev.probe.internal.ProbeServices
import kotlin.concurrent.Volatile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.runBlocking

@OptIn(InternalCoroutinesApi::class)
object ProbeRuntime {
    private val lock = SynchronizedObject()

    @Volatile
    private var services: ProbeServices? = null

    fun initialize(config: ProbeConfig, platform: ProbePlatformContext, scope: CoroutineScope) {
        if (services != null) return
        synchronized(lock) {
            if (services != null) return
            services = ProbeGraphFactory.create(config, platform, scope)
            ProbeState.setCallbacks(
                object : ProbeState.Callbacks {
                    override fun onAppBackgrounded() = this@ProbeRuntime.onAppBackgrounded()

                    override fun onAppForegrounded() = this@ProbeRuntime.onAppForegrounded()
                },
            )
            ProbeHub.setHook(
                object : ProbeHub.Hook {
                    override fun isEnabled(): Boolean = this@ProbeRuntime.isEnabled()

                    override fun openHub(context: ProbePlatformContext) = ProbeLauncher.openHub(context)
                },
            )
        }
    }

    /**
     * Synchronous by design so callers on any thread can tear the runtime down deterministically.
     * The browser controller's `stop()` is suspend (its own shutdown work is IO-dispatched and
     * non-blocking); [runBlocking] bridges into it here rather than making [shutdown] itself
     * suspend, which would push a suspend requirement onto every call site of this object function.
     */
    fun shutdown() {
        synchronized(lock) {
            services?.notifierBridge?.stop()
            services?.outputController?.cancelPendingWork()
            runBlocking { services?.browserController?.stop() }
            ProbeHttpCapture.setHook(NoOpHttpClientDebugHook)
            ProbeState.clearCallbacks()
            ProbeHub.clearHook()
            ProbePlatformHolder.clear()
            ProbeDatabaseCapture.unregister(PROBE_SELF_DATABASE_NAME)
            ProbeLogSink.clearWriter()
            services?.uninstallCrashHook?.invoke()
            ProbeCrashCapture.clearReporter()
            services = null
        }
    }

    /**
     * Called from the host app's process-lifetime lifecycle observer on backgrounding. Non-
     * blocking: [com.dev.probe.browser.NetworkBrowserController.onLifecyclePause] itself
     * launches its stop work on its own scope rather than suspending here, since this is invoked
     * from the app's main/UI lifecycle callback and must never block it. No-op when probe-runtime is
     * disabled or the browser was never started.
     */
    fun onAppBackgrounded() {
        services?.browserController?.onLifecyclePause()
    }

    /**
     * Called from the host app's process-lifetime lifecycle observer on returning to the
     * foreground. Re-applies the persisted output mode so a server that [onAppBackgrounded]
     * auto-stopped comes back — [scheduleRestore] is already fire-and-forget, matching the
     * non-blocking contract this function shares with [onAppBackgrounded].
     */
    fun onAppForegrounded() {
        services?.outputController?.scheduleRestore()
    }

    /** Returns `false` before [initialize] rather than throwing, unlike [services]. */
    fun isEnabled(): Boolean = services?.config?.isEnabled() ?: false

    internal fun services(): ProbeServices = checkNotNull(services) {
        "ProbeRuntime.initialize() must be called before accessing debug services"
    }
}
