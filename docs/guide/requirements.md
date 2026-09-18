← [Guide index](README.md)

# Requirements

- **Platforms:** Android and iOS, via Kotlin Multiplatform. No other targets are declared.
- **Kotlin:** 2.4.0 in this repository's version catalog (`gradle/libs.versions.toml`) — not
  independently verified as a hard minimum for probe itself, since it hasn't been tested against
  other Kotlin versions.
- **Android:** `minSdk` 28, `compileSdk` 37 in this repository's catalog. AGP 9.2.1, via the
  `com.android.kotlin.multiplatform.library` plugin for `:probe-runtime`/`:probe-api` themselves,
  and a real `com.android.application` module (with a genuine `buildTypes {}` axis) for the host
  app that wants build-type-based exclusion.
- **iOS:** targets `iosArm64` and `iosSimulatorArm64`. No `iosX64` (Intel simulator) target is
  declared.
- **UI toolkit:** Compose Multiplatform (Material 3). The debug shell's UI is built entirely in
  Compose, including on iOS (via `ComposeUIViewController`).
- **Networking:** Ktor 3.5.0 in this catalog — `ktor-client-core` (capture hook) and, inside
  `:probe-runtime`, `ktor-server-cio` + `ktor-server-websockets` (the embedded browser server).
- **Dependency injection:** none required. Integration is a plain function call
  (`ProbeInstaller.install`/`installProbeTools`, see [integration-guide.md](integration-guide.md)) —
  no DI framework involvement, Koin or otherwise.
- **Android-only:** `androidx.startup:startup-runtime` 1.2.0 — used for `ProbeStartupInitializer`,
  which self-registers via a manifest-merged `ContentProvider` so the install hook is ready before
  `Application.onCreate()`.
- **Storage:** Room (`androidx.room`, KSP-generated) for captured network calls and sessions;
  Jetpack DataStore Preferences for debug preferences (network output mode). Both work
  cross-platform via KMP artifacts — no platform-specific database code beyond builder wiring.
  `:probe-api` also depends on `androidx.room:room-runtime` (as `api`, no KSP/codegen) purely for
  the `RoomDatabase` type in `ProbeDatabaseCapture.register`'s signature — every consuming app's
  release build carries this dependency, whether or not it uses Room itself, a deliberate,
  accepted cost documented in the Database Capture design spec.
- **Development-only additional dependency:** `dev.icerock.moko:permissions` (moko-permissions),
  used only by the Permissions dev panel.
- **Not independently verifiable from this repository:** minimum iOS deployment target and
  minimum Xcode/Swift toolchain version for `:probe-runtime`'s own KMP targets specifically (the host
  app in this repository targets iOS 15.0, but that is a host-app setting, not something
  `:probe-runtime`'s build file pins itself).

---
← [Guide index](README.md) · Previous: [Architecture](architecture.md) · Next: [Integration guide](integration-guide.md)
