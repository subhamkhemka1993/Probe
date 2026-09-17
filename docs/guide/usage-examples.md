← [Guide index](README.md)

# Usage Examples

**Initialize on Android** (once, from code that runs in every build type):
```kotlin
ProbeInstaller.install(
    config = ProbeConfig(isEnabled = { BuildConfig.DEBUG }),
    platform = ProbePlatformContext(context),
)
```

**Initialize on iOS** (once, as early as possible):
```kotlin
installProbeTools(
    config = ProbeConfig(isEnabled = { true }),
    platform = ProbePlatformContext(),
)
```

**Open the debug hub programmatically** (e.g. from a debug-only menu item) — safe to call even
when `:probe-runtime` might be absent:
```kotlin
ProbeHub.openHub(platformContext)
```

**Open straight into the network inspector:**
```kotlin
ProbeLauncher.openInspector(platformContext)
```

**Install network capture on a Ktor client:**
```kotlin
val client = HttpClient(engine) {
    ProbeHttpCapture.run { installCapture() }
}
```

**Wire app-lifecycle events** (stop the browser server / restore output mode automatically):
```kotlin
val observer = LifecycleEventObserver { _, event ->
    when (event) {
        Lifecycle.Event.ON_STOP -> ProbeState.onAppBackgrounded()
        Lifecycle.Event.ON_START -> ProbeState.onAppForegrounded()
        else -> Unit
    }
}
```

**Configure with a brand-matched theme and a remote-config-backed flag:**
```kotlin
val config = ProbeConfig(
    isEnabled = { BuildConfig.DEBUG && myRemoteConfig.debugToolsAllowed },
    themeOverride = ProbeThemeOverride(primary = MyBrand.primary, error = MyBrand.error),
)
```

**Register a host-storage clear callback** (relevant on iOS, where there's no full OS reset):
```kotlin
ProbeHostCallbacks.onClearScopedData = {
    myAuthTokenStore.clear()
    listOf("Auth tokens")
}
```

---
← [Guide index](README.md) · Previous: [Configuration reference](configuration-reference.md) · Next: [Security](security.md)
