← [Guide index](README.md)

# Android Integration

**Installation:** as in [integration-guide.md, Step 1](integration-guide.md#step-1--add-probe-to-your-project) —
`implementation(projects.probeApi)` unconditionally, `debugImplementation(projects.probeRuntime)`
for the heavy module. Because `ProbeInstaller.install(...)` is a safe no-op when `:probe-runtime`
isn't on the classpath (see [architecture.md](architecture.md)), a project with additional non-`debug`,
non-`release` build types (e.g. an internal-distribution build type) needs **no per-build-type
code at all** — unlike the old Koin-module-per-build-type pattern this replaced, there's no empty
counterpart file to remember to add for a third build type. The one `ProbeInstaller.install(...)`
call is written once and compiled into every build type unconditionally.

**Initialization:** exactly one call to `ProbeInstaller.install(config, platform)`, from code that
runs on every build variant. This repository's own sample calls it from `MainActivity.onCreate()`:

```kotlin
// androidApp/src/main/kotlin/.../MainActivity.kt — this repository's real sample
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ProbeInstaller.install(
            config = ProbeConfig(isEnabled = { true }),
            platform = ProbePlatformContext(this),
        )

        setContent {
            App(onOpenHub = { ProbeHub.openHub(ProbePlatformContext(this)) })
        }
    }
}
```

A real app would more likely make this call from `Application.onCreate()` rather than an
`Activity`, so it fires exactly once per process regardless of which screen launches first — the
sample uses `MainActivity` only because the wizard-generated sample has no custom `Application`
class.

**Ktor capture wiring** — identical to iOS, in your `HttpClientConfig` builder:
`ProbeHttpCapture.run { installCapture() }`.

**App-lifecycle wiring** — identical to iOS:
`ProbeState.onAppBackgrounded()` / `ProbeState.onAppForegrounded()` from your
`LifecycleEventObserver`.

**Manifest merge:** `:probe-runtime`'s own `AndroidManifest.xml` declares its Activities, a
`FileProvider`, a launcher `activity-alias`, and the App Startup `<provider>` entry that registers
`ProbeStartupInitializer` — all of this merges into your app's manifest automatically wherever
`:probe-runtime` is on the classpath, no manual manifest work needed on your part. It does require
`POST_NOTIFICATIONS` (declared by `:probe-runtime`'s manifest) for the sticky capture notification
on Android 13+.

**Opening the shell:** `ProbeLauncher.openHub(ProbePlatformContext(context))` starts a dedicated
`Activity` (`ProbeActivity`) via `Intent` with `FLAG_ACTIVITY_NEW_TASK`.

**Home-screen launcher icon.** `:probe-runtime`'s manifest declares an `activity-alias`
(`ProbeLauncherAlias`, targeting `ProbeActivity`) with a `MAIN`/`LAUNCHER` intent filter and its
own icon/label — so wherever `:probe-runtime` is on the classpath (i.e. debug builds, in the pattern
above), a second "Probe" icon appears on the home screen/app drawer, independent of any
in-app trigger. Because this is a static manifest entry (always `android:enabled="true"`), the
target activity (`ProbeActivity`) itself checks `ProbeRuntime.isEnabled()` on `onCreate` and
immediately finishes if false — this is what keeps `ProbeConfig.isEnabled` authoritative over
reachability even through this always-present icon.

**Clear app data on Android** is a real, full reset via
`ActivityManager.clearApplicationUserData()` — the process is killed and restarted by the OS
shortly after.

**Android-specific limitations:**
- The Permissions dev panel only resolves real status for permissions your app has actually
  declared in its manifest (`POST_NOTIFICATIONS`, `CAMERA`, `ACCESS_FINE_LOCATION`) — anything
  else reports "Not declared by host app."
- The notification-permission request flow requires a transparent activity
  (`ProbeNotificationPermissionActivity`) since a plain notifier object can't launch a system
  permission dialog — this is handled internally, no host wiring needed.

---
← [Guide index](README.md) · Previous: [Integration guide](integration-guide.md) · Next: [iOS integration](integration-ios.md)
