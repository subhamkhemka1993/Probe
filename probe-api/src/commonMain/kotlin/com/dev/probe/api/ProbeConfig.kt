package com.dev.probe.api

/**
 * The single configuration seam a host app uses to wire this library to its own build/flag
 * system. [isEnabled] is evaluated on every relevant call (not cached at construction), so it's
 * safe to back it with a runtime flag your build system can't prove statically false/true — e.g.
 * `{ BuildConfig.DEBUG && myRemoteConfig.debugToolsAllowed }`.
 *
 * This is intentionally the *only* configuration mechanism the library exposes — don't add a
 * second, competing one alongside it.
 */
data class ProbeConfig(val isEnabled: () -> Boolean, val themeOverride: ProbeThemeOverride? = null)
