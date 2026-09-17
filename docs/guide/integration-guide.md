← [Guide index](README.md)

# Integration Guide

probe is currently integrated as **local Gradle/KMP source modules** (`:probe-api`,
`:probe-runtime`), not as a published package (no Maven coordinates, no CocoaPods podspec, no Swift
Package Manager manifest exist for it). To use it in a different project today, you would copy
both module directories into that project's Gradle build and `include()` them in
`settings.gradle.kts`, adjusting package names as desired. Publishing it as a standalone Maven
artifact (so other projects could add a `implementation("...:probe-api:x.y.z")` dependency
instead of vendoring source) would be a reasonable next step but has not been done.

## Step 1 — Add probe to your project

```kotlin
// settings.gradle.kts
include(":probe-api")
include(":probe-runtime")
```

Then, from every module that needs to call into probe at runtime (typically your shared/common
module and your networking module), depend on `:probe-api` unconditionally:

```kotlin
// your shared module's build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.probeApi)
        }
    }
}
```

Depend on `:probe-runtime` (the heavy implementation) only from the build configuration(s) where you
actually want it present. On Android, that means Gradle's `debugImplementation` on your
application module:

```kotlin
// your application module's build.gradle.kts
dependencies {
    implementation(projects.probeApi)
    debugImplementation(projects.probeRuntime)
}
```

On iOS, Kotlin/Native has no build-type dependency axis analogous to Android's `debug`/`release`
source sets, so `:probe-runtime` is instead depended on unconditionally from your shared module's
`iosMain` source set, and gated purely by the runtime flag described in Step 3:

```kotlin
kotlin {
    sourceSets {
        iosMain.dependencies {
            implementation(projects.probeRuntime)
        }
    }
}
```

## Step 2 — Initialize probe

Initialization is a plain function call — no dependency-injection framework required. The
mechanism differs slightly per platform because only Android has a way to run code before
`Application.onCreate()`. See [integration-android.md](integration-android.md) and
[integration-ios.md](integration-ios.md) for the full per-platform walkthrough; in short:

- **Android:** `ProbeInstaller.install(config, platform)`, called once from code that runs on
  every build variant.
- **iOS:** `installProbeTools(config, platform)`, called once, as early as possible.

Both calls end up doing the same thing under the hood: building the full dependency graph exactly
once per process and installing the hooks described in [architecture.md](architecture.md). There is no
other supported initialization path — calling `ProbeRuntime.initialize(...)` directly is possible
(it's a public function) but bypasses the one-time-call guarantee `ProbeInstaller`/
`installProbeTools` give you for free, and is not documented or exercised by any example here.

## Step 3 — Configure

`ProbeConfig` is the **only** configuration type probe exposes — deliberately, so a host never
needs a second, competing configuration mechanism. See
[configuration-reference.md](configuration-reference.md) for the full field reference.

| Option | Type | Default | Description |
|---|---|---|---|
| `isEnabled` | `() -> Boolean` | *(required)* | Evaluated on every relevant call, not cached — safe to back with a runtime flag (e.g. a remote-config value) your build system can't prove statically true/false. Gates whether the network capture hook, browser server, and sticky notifier actually do anything; the Compose UI is still reachable regardless (see [troubleshooting-and-faq.md](troubleshooting-and-faq.md#usage-gaps)). |
| `themeOverride` | `ProbeThemeOverride?` | `null` | Optional palette override for the debug shell's own UI. |

## Step 4 — Enable/disable

- probe's implementation module (`:probe-runtime`) should be present only in builds where you want
  it reachable at all. This repository's pattern: Android — `debugImplementation`; iOS — always
  present, gated at runtime (there is no Android-style build-type axis on Kotlin/Native targets).
- Independent of module presence, `ProbeConfig.isEnabled` is the runtime kill switch. It's
  evaluated dynamically, so it's safe to combine a debug-build check with a feature flag, e.g.
  `{ BuildConfig.DEBUG && myRemoteConfig.debugToolsAllowed }`.
- Release builds should not include `:probe-runtime` at all if you want a structural guarantee (see
  [security.md](security.md)) rather than relying solely on `isEnabled` returning `false`.

## Step 5 — Open the developer tools

There is no gesture (shake, multi-finger tap) built into probe. The supported entry points are:

- **Programmatic (recommended):** `ProbeHub.openHub(platformContext)` — from `:probe-api`, so it's
  safe to call unconditionally even in a build where `:probe-runtime` is absent (silent no-op,
  same settable-hook pattern as `ProbeHttpCapture`/`ProbeState`). This repository's own sample app
  calls this from its "Open Probe Hub" button.
- **Programmatic (lower-level):** `ProbeLauncher.openHub(platformContext)` /
  `ProbeLauncher.openInspector(platformContext)` — from `:probe-runtime` directly. Only call this
  from code you already know only runs where `:probe-runtime` is present; unlike `ProbeHub`, it has
  no no-op fallback. On Android this starts a dedicated `Activity`; on iOS it presents a
  full-screen `UIViewController` on top of the current view controller stack.
- **Tapping the persistent capture notification**, which is wired automatically once capture is
  active — see [capability-reference.md](capability-reference.md).
- **On Android only:** tapping a "Probe" home-screen icon (an `activity-alias` declared in
  `:probe-runtime`'s own manifest) — see [integration-android.md](integration-android.md).

---
← [Guide index](README.md) · Previous: [Requirements](requirements.md) · Next: [Android integration](integration-android.md)
