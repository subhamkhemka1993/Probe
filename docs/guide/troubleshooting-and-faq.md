← [Guide index](README.md)

# Troubleshooting & FAQ

## Troubleshooting

**probe doesn't appear / tapping the launcher/notification does nothing.**
- Confirm `ProbeConfig.isEnabled` currently returns `true` — it's evaluated live, so a build-type
  check alone (e.g. `BuildConfig.DEBUG`) won't help if a paired runtime flag is off.
- On Android, confirm you're running a build variant where `:probe-runtime` is actually on the
  classpath (e.g. `debugImplementation` was used, and this is a `debug`-family build, not
  `release`/`internalSharing`).
- Confirm `ProbeInstaller.install(config, platform)` (Android) or `installProbeTools(config,
  platform)` (iOS) has actually been called — see [integration-guide.md, Step 2](integration-guide.md#step-2--initialize-probe).
  Neither happens automatically just from adding the module dependency.

**Initialization seems to fail / calling into probe throws.**
- `ProbeRuntime.services()` throws `IllegalStateException` if called before
  `ProbeRuntime.initialize()` has run — check that `ProbeInstaller.install(...)`/
  `installProbeTools(...)` actually executes before any probe UI is reachable.
- `ProbeHttpCapture`/`ProbeState` never throw — they're safe no-ops before initialization by
  design, so if network capture "isn't working," this is not the failure mode; check `isEnabled`
  instead.

**Network calls are missing from the inspector.**
- Confirm `ProbeHttpCapture.run { installCapture() }` (or the equivalent extension-function call)
  is actually present in the `HttpClientConfig` builder for the client you're testing — a client
  built without this call is invisible to probe, by design.
- Confirm you're viewing the session you expect ("Current" vs. "Previous" chip) — a request made
  in a prior process only shows up under "Previous."
- Binary-content-type responses are recorded as `[binary body omitted]`, not with a real body —
  this is expected, not a bug.

**The browser inspector shows "Starting server" indefinitely, or a bind error.**
- Check `BrowserConnectionInfo.Error`'s message (surfaced in the mode-picker card) — a bind
  failure (e.g. port `8765` already in use) will show there.
- Confirm the device/emulator/simulator and your desktop are actually on the same network for a
  real device; for an Android emulator, you need `adb forward tcp:8765 tcp:8765` first (shown
  inline in the UI's "Emulator desktop access" accordion).

**Notification tap doesn't open the hub (iOS only).**
- Confirm your `AppDelegate`'s notification-response handler actually calls into a bridge that
  checks the identifier and calls `ProbeLauncher.openHub(...)` — this wiring is not automatic on
  iOS (see [integration-ios.md](integration-ios.md)).

**A permission always shows "Not declared by host app."**
- This is expected if your app's manifest (Android) or `Info.plist` (iOS) genuinely doesn't
  declare that permission — probe can only report status for permissions the host has declared.

## FAQ

**Is probe intended for production?** No. It's a developer/debugging tool intended to be absent
or disabled in builds a non-developer would use. On Android, absence can be made structural
(recommended); on iOS, disabling relies entirely on `ProbeConfig.isEnabled`.

**Which platforms are supported?** Android and iOS, via Kotlin Multiplatform + Compose
Multiplatform. No other platform targets exist in this codebase.

**Does it require changes to the host application?** Yes: adding the module dependencies, calling
the install entry point (`ProbeInstaller.install`/`installProbeTools`), wiring the Ktor capture
hook, and wiring an app-lifecycle observer are all required integration steps — none of this
happens automatically just by adding the dependency. On Android, hook *registration* (not
initialization) is the one piece that does happen automatically, via App Startup.

**How is probe enabled?** Via `ProbeConfig.isEnabled`, a caller-supplied predicate re-evaluated
on every relevant call — plus, on Android, whether `:probe-runtime` is even on the current build
variant's classpath.

**Does it capture all network traffic?** Only traffic through a Ktor `HttpClient` instance that
explicitly installed the capture hook. Traffic through any other client (a different Ktor
instance without the hook, a raw socket, a third-party SDK's own networking) is invisible to it.

**Where is debug data stored?** Locally on-device: a Room database for captured calls/sessions,
DataStore Preferences for settings. Nothing is sent off-device by probe itself except via the
explicit export/share action, or via the embedded browser server while a client is connected to
it.

**Can projects extend it?** Yes, via the `ProbeInspector`/`ProbePlugin` interfaces — see
[extensibility.md](extensibility.md). There's no plugin-marketplace or dynamic-loading mechanism;
extension means adding a Kotlin class and registering it in the plugin list `ProbeGraphFactory`
assembles.

**What differs between iOS and Android?** See the [feature matrix](capability-reference.md#ios-vs-android-feature-matrix)
above and the Usage Gaps section below — the two most consequential differences are that iOS has
no structural production-exclusion equivalent to Android's build-type exclusion, and that iOS's
notification-tap entry point requires host-app glue code that Android's doesn't.

## Usage Gaps

### Confirmed gaps

**iOS has no structural production-exclusion mechanism.** Android's release safety comes from
`:probe-runtime` being absent from the release *dependency graph*, verified by a dedicated Gradle task
in the host app that inspects release runtime classpaths. iOS ships `:probe-runtime` in every build
configuration and relies entirely on `ProbeConfig.isEnabled` evaluating to `false`. This is a real
platform asymmetry inherent to Kotlin/Native having no build-type axis, not a code defect — but
it means an iOS release build's binary always contains the full debug-tool implementation, gated
by a runtime check rather than by structural absence.
*Impact:* if a host's `isEnabled` predicate is ever miscomputed or bypassed on iOS, there is no
second line of defense.

**iOS notification-tap routing requires host-app glue code; Android's does not.** Android's
`PendingIntent` targets `ProbeActivity` directly — no host involvement required. iOS notification
taps route through the host's own `UNUserNotificationCenterDelegate`, so every iOS host must write
and maintain its own small bridge object (as documented in [integration-ios.md](integration-ios.md)).
This is a real, ongoing integration burden difference, not a missing feature — but it means the
"notification tap opens the hub" capability is not zero-effort on iOS the way it is on Android.
*Evidence:* `PROBE_NOTIFICATION_ID`
(`probe-api/src/commonMain/kotlin/com/dev/probe/api/ProbeNotificationId.kt`) exists specifically
so a host's own notification-response handler can recognize a tap on Probe's notification and
route it to `ProbeHub.openHub(...)` — a value `:probe-runtime` couldn't route itself, since
routing happens inside the host's `UNUserNotificationCenterDelegate`, not inside Probe.

**No home-screen launcher icon on iOS.** Confirmed by the absence of any iOS equivalent to the
Android `activity-alias` in `probe-runtime/src/androidMain/AndroidManifest.xml` — iOS apps cannot
register a second home-screen icon pointing into a different part of the same app via a manifest
mechanism, so this specific UX (tap an icon, skip any in-app trigger) has no iOS counterpart in
this implementation.

**`isEnabled` gates capture/server/notifier construction, but not the debug shell's UI
rendering itself, on the path a caller directly reaches.** `ProbeGraphFactory.create()` only
starts the real hook, browser server, and notifier `if (config.isEnabled())` — but it *always*
constructs the graph and returns a working `ProbeServices`, and neither `ProbeApp` (the Compose
shell) nor the iOS `createProbeViewController` checks `isEnabled()` before rendering. On Android
this is fully mitigated for the one reachable-without-an-explicit-trigger path (the new launcher
icon) by the `ProbeActivity.onCreate()` guard added specifically for that path. On iOS, the
equivalent guard was not added to `createProbeViewController`/`ProbeViewControllerPresenter`,
because iOS's only entry point (a notification tap) is already gated one level up, in
`ProbeNotificationBridge.isProbeNotification` checking `ProbeRuntime.isEnabled()` before ever
calling `ProbeLauncher.openHub`. This is consistent today, but it means the enforcement point is
inconsistent across platforms (in the target activity/view-controller on Android, in the caller on
iOS) — a new iOS entry point added later without replicating that same caller-side check would not
inherit any protection from `:probe-runtime` itself.

### Potential gaps (inconsistent or suspicious, not conclusively classified)

**`ProbeHostCallbacks.onClearScopedData` is iOS-invoked only, with no equivalent registration
point used on Android.** It's a common (`commonMain`) settable hook, but only iOS's
`clearAppData` actual implementation ever calls it; Android's full-reset path has no use for it
since `ActivityManager.clearApplicationUserData()` already clears everything. This is most likely
intentional (Android doesn't need a partial-clear callback when it can do a full reset), but the
API being declared in shared code with only one platform consumer is at least worth a host
integrator's attention if they register a callback expecting it to run on both platforms.

**Browser output mode's "restore on foreground" path (`NetworkOutputController.scheduleRestore`)
runs unconditionally on `ProbeState.onAppForegrounded()`, regardless of `isEnabled`.**
`ProbeRuntime.onAppForegrounded()` calls `outputController.scheduleRestore()` with no
`isEnabled()` check, unlike `ProbeGraphFactory.create()`'s other conditional calls. In practice
this is likely harmless (the browser controller's own `start()`/`stop()` each independently check
`isEnabled()` before doing anything), but it means a disabled-but-initialized instance still does
some scheduling work on every foreground transition rather than being a complete no-op.

**No automated test found that exercises the `activity-alias` → `ProbeActivity.isEnabled()` guard
end-to-end.** The guard is a two-line, easily-reasoned-about check, but nothing in
`probe-runtime/src/androidHostTest` specifically instantiates `ProbeActivity` with `isEnabled()`
false to assert the activity finishes without rendering — this module's `build.gradle.kts` does
not declare an `androidDeviceTest` source set at all (only `withHostTestBuilder`), so there is
also no instrumented-test location where such a test could live without adding one. Flagging this
as a coverage gap rather than a confirmed defect.

---
← [Guide index](README.md) · Previous: [Security](security.md) · Next: [Extensibility](extensibility.md)
