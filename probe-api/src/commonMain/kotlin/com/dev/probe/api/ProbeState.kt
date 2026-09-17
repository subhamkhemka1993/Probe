package com.dev.probe.api

import kotlin.concurrent.Volatile

/**
 * Host integration point for the debug shell's app-lifecycle observer. [AppViewModel]-equivalent
 * code in the host app calls [onAppBackgrounded]/[onAppForegrounded] unconditionally;
 * `:probe-runtime`'s `ProbeRuntime.initialize` installs the real [Callbacks], defaulting to a no-op
 * otherwise (same settable-hook pattern as [ProbeHttpCapture]).
 */
object ProbeState {
    interface Callbacks {
        fun onAppBackgrounded()

        fun onAppForegrounded()
    }

    private object NoOpCallbacks : Callbacks {
        override fun onAppBackgrounded() = Unit

        override fun onAppForegrounded() = Unit
    }

    @Volatile
    private var callbacks: Callbacks = NoOpCallbacks

    fun setCallbacks(callbacks: Callbacks) {
        this.callbacks = callbacks
    }

    fun clearCallbacks() {
        callbacks = NoOpCallbacks
    }

    fun onAppBackgrounded() = callbacks.onAppBackgrounded()

    fun onAppForegrounded() = callbacks.onAppForegrounded()
}
