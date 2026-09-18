← [Guide index](README.md)

# Development

**Repository structure** (within `:probe-runtime`):
- `probe-runtime/src/commonMain` — shared implementation: network inspector, browser server, DB,
  prefs, session manager, dev actions, Compose UI.
- `probe-runtime/src/androidMain` / `iosMain` — platform actuals (notifier, launcher, address
  discovery, share sheet, permissions, database/DataStore builders) plus, on Android only,
  `startup/ProbeStartupInitializer.kt` (the App Startup self-registration).
- `probe-runtime/src/commonMain/.../startup/InstallProbeTools.kt` — the direct, non-Android
  initialization entry point (`installProbeTools`), shared by iOS and by
  `ProbeStartupInitializer`'s own registered hook on Android.
- `probe-runtime/src/commonTest` / `androidHostTest` / `iosTest` — unit tests (JVM-based `commonTest`
  and `androidHostTest` cover the large majority; a small `iosTest` set exists for the
  Room/DataStore-backed repository on the iOS target specifically).
- `probe-api/src/commonMain` / `commonTest` — the always-present hook API and its tests. Its
  `build.gradle.kts` depends on `androidx.room:room-runtime` (`api`, no KSP) purely for the
  `RoomDatabase` type in `ProbeDatabaseCapture` — see [requirements.md](requirements.md).

**Build:**
```
./gradlew :probe-runtime:compileKotlinMetadata :probe-api:compileKotlinMetadata
```

**Test:**
```
./gradlew :probe-runtime:testAndroidHostTest :probe-api:testAndroidHostTest
```
(`commonTest`/`iosTest` targets run via the platform-specific test tasks Kotlin Multiplatform
generates, e.g. `:probe-runtime:iosSimulatorArm64Test`.)

**Format & lint (all modules):**
```
./gradlew spotlessCheck   # or spotlessApply to auto-fix
./gradlew lint
```
See [../coding-guardrails.md](../coding-guardrails.md) for the full Spotless/ktlint/Android Lint
conventions and how to handle a new finding.

**Adding a feature:** most new capabilities belong in `commonMain` with `expect`/`actual` only for
the genuinely platform-specific edge (as the existing code does for notifications, address
discovery, sharing, and permissions) — this keeps the iOS/Android feature matrix from drifting
further apart than the platforms themselves force it to.

**Before committing:** if your change touches a source path listed in
[`../../scripts/docs-sources.conf`](../../scripts/docs-sources.conf), update the paired guide file
in this directory in the same commit — see [../git-guide.md](../git-guide.md#keeping-docs-in-sync).

**No CONTRIBUTING.md or explicit contribution-process document was found for this module** — none
is claimed here beyond what's inferable from the existing test layout and module boundaries above.

---
← [Guide index](README.md) · Previous: [Extensibility](extensibility.md) · Next: [Publishing](publishing.md)
