← [Guide index](README.md)

# iOS Integration

**Installation:** as in [integration-guide.md, Step 1](integration-guide.md#step-1--add-probe-to-your-project) —
`:probe-api` from your shared `commonMain`, `:probe-runtime` from your shared module's `iosMain`.

**Initialization:** a direct call to `installProbeTools(config, platform)` — see
[integration-guide.md, Step 2](integration-guide.md#step-2--initialize-probe) — made as early as
possible, before any Probe UI could be reached. Because Swift can't call a top-level Kotlin
function without a wrapper object, this repository's own sample wraps it in a small
`commonMain`/`iosMain` function and calls that from `iOSApp.swift`'s `init()`:

```kotlin
// shared/src/iosMain/kotlin/.../ProbeBootstrap.kt — this repository's real sample wrapper
internal var sampleAppResources: SampleAppResources? = null

fun installProbeSample() {
    val platform = ProbePlatformContext()
    installProbeTools(
        config = ProbeConfig(isEnabled = { true }),
        platform = platform,
    )
    sampleAppResources = installSampleResources(platform)
}
```

`installSampleResources` (in `shared`'s `com.dev.probe.sample` package, not part of Probe itself)
is this sample app's own demo code — see the equivalent note in
[integration-android.md](integration-android.md).

```swift
// iosApp/iosApp/iOSApp.swift — this repository's real sample
@main
struct iOSApp: App {
    init() {
        ProbeBootstrapKt.installProbeSample()
    }
    // ...
}
```

Calling it from `init()` (rather than, say, `AppDelegate.swift`'s
`didFinishLaunchingWithOptions`) matters: `init()` runs first, so Probe is ready before any UI —
including your own app's first screen — has a chance to run.

**Ktor capture wiring**, in your `HttpClientConfig` builder:

```kotlin
HttpClient(engine) {
    ProbeHttpCapture.run { installCapture() }
    // ... your other client config
}
```

**App-lifecycle wiring**, in your process-lifetime lifecycle observer:

```kotlin
when (event) {
    Lifecycle.Event.ON_STOP -> ProbeState.onAppBackgrounded()
    Lifecycle.Event.ON_START -> ProbeState.onAppForegrounded()
}
```

**Opening the shell:** `ProbeLauncher.openHub(ProbePlatformContext())` presents probe
full-screen (`UIModalPresentationFullScreen`) on top of the current key window's topmost view
controller (walking through any presented, `UINavigationController`, or `UITabBarController`
chain). It auto-dismisses once you close the hub/inspector from inside the shell.

**Notification tap requires host wiring on iOS**, because notification-tap routing goes through
your own `UNUserNotificationCenterDelegate`, not through probe. This is not something a sample
app in this repository currently exercises (the sample's own notification handling, if any, is
out of scope of this module) — the pattern below is illustrative, not a link to real code here:
a small Objective-C-visible object exposing only `String`/`Boolean`/`Unit` (so no `:probe-runtime`
type has to cross your framework's export boundary), called from your notification-response
handler:

```kotlin
// Illustrative — a pattern a host app would write, not part of probe itself
object MyProbeNotificationBridge {
    fun isProbeNotification(identifier: String): Boolean =
        identifier == PROBE_NOTIFICATION_ID && ProbeHub.isEnabled()

    fun openHub() = ProbeHub.openHub(ProbePlatformContext())
}
```
```swift
// AppDelegate.swift — illustrative
if MyProbeNotificationBridge.shared.isProbeNotification(identifier: response.notification.request.identifier) {
    MyProbeNotificationBridge.shared.openHub()
}
```

**Clear app data on iOS** cannot fully reset app state (no OS API for it). It clears probe's own
storage and anything registered via `ProbeHostCallbacks.onClearScopedData` — see
[extensibility.md](extensibility.md).

**iOS-specific limitations:**
- No home-screen launcher icon equivalent to Android's `activity-alias` — the only entry points
  are programmatic (`ProbeLauncher`) and the notification tap (which requires the host wiring
  above).
- No `ActivityManager`-style full process reset.
- Notification permission is requested with `UNAuthorizationOptionProvisional` (quiet delivery,
  no permission dialog) specifically so it never pre-empts your app's own onboarding permission
  prompt — but this means the notification may not be visible unless the user has enabled it, or
  may appear only in Notification Center rather than as a banner, depending on iOS settings.

---
← [Guide index](README.md) · Previous: [Android integration](integration-android.md) · Next: [Capability reference](capability-reference.md)
