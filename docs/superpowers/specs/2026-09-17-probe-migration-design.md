# Probe: Standalone Extraction of zdebug-api/zdevtools — Design

**Status:** Approved for planning
**Source app:** `zebpay_multiplatform` (`feat/zdevtools-inapp-debug-framework`, merged commit `4cbc38806`, PR #807)
**Target project:** `/Users/dianapps/StudioProjects/Probe` (fresh KMP wizard scaffold, currently ungitted)

## Goal

Extract the existing, working `zdebug-api`/`zdevtools` in-app debug-tool module pair out of
`zebpay_multiplatform` into Probe: an independent Kotlin Multiplatform + Compose Multiplatform
project, so it can eventually be consumed as an external dependency by zebpay (and, longer term,
by other apps) instead of living inline in the app's own module graph.

This is a **migration**, not a rewrite: the source implementation is already tested and running
in production-adjacent code. The work is a mechanical port plus package/symbol rename plus
version alignment, not new feature design.

## Non-goals (explicitly out of scope for this pass)

- The archived DB/DataStore/Log capture spike (`feat/zdebug-runtime-state-capture` in
  `zebpay_multiplatform`, commit `e91dacdb9`) is **not** part of this migration. It stays parked
  in `zebpay_multiplatform` and gets revisited separately, on top of Probe, once Probe exists.
- Deciding *how* `zebpay_multiplatform` will eventually consume Probe (Maven/GitHub Packages vs.
  composite build vs. something else) is deferred. This pass only gets Probe standing on its own.
- No new features. Behavior should be identical to what `zdevtools`/`zdebug-api` do today, modulo
  the rename.

## Architecture

Two-module KMP split, mirroring the pattern already proven in `zebpay_multiplatform`:

- **`probe-api`** (was `zdebug-api`) — tiny, always-`implementation`-safe surface. Settable,
  no-op-by-default hooks: `ProbeHub`, `ProbeInstaller`, `ProbeState`, `ProbeHttpCapture`,
  `ProbePlatformContext`, `PROBE_NOTIFICATION_ID`, `ProbeConfig`. Safe to ship in every build
  variant, including release, because every hook defaults to doing nothing until a runtime
  registers itself.
- **`probe-runtime`** (was `zdevtools`) — the real implementation: Compose Multiplatform debug
  UI, embedded Ktor server for browser-based network inspection, Room-backed capture storage,
  moko-permission usage, Android App Startup self-registration. Only included in debug-type
  builds by a consumer app.

`androidApp`/`iosApp` (currently wizard-default Greeting screens) become the **sample/integration
app** — the same role `androidApp`/`zebpayApp` play for `zdevtools` today: self-install on
launch, expose a way to open the debug hub, and serve as the manual test bed and living
integration documentation.

## Module & package layout

```
Probe/
├── probe-api/       package: com.dev.probe.api
├── probe-runtime/   package: com.dev.probe   (+ .startup, .browser, .dbinspector, .datastore, .logs sub-packages)
├── androidApp/       — sample app: self-installs probe-runtime via App Startup, button to open the hub
├── iosApp/           — sample app: calls the ProbeIosBootstrap-equivalent from iOSApp.swift's init()
```

### Rename table

| zebpay_multiplatform | Probe |
|---|---|
| `com.zebpay.devtools.api` (package) | `com.dev.probe.api` |
| `com.zebpay.devtools` (package) | `com.dev.probe` |
| `ZDebugHub` | `ProbeHub` |
| `ZDebugInstaller` | `ProbeInstaller` |
| `ZDebugRuntime` | `ProbeRuntime` |
| `ZDebugState` | `ProbeState` |
| `ZDebugHttpCapture` | `ProbeHttpCapture` |
| `ZDebugPlatformContext` | `ProbePlatformContext` |
| `ZToolConfig` | `ProbeConfig` |
| `ZDebugStartupInitializer` | `ProbeStartupInitializer` |
| `ZDEBUG_NOTIFICATION_ID` | `PROBE_NOTIFICATION_ID` |
| `installZDebugTools(...)` | `installProbeTools(...)` |
| `ZDebugLauncher` | `ProbeLauncher` |
| `ZDebugGraphFactory` / `ZDebugServices` / `ZDebugPlatformHolder` (internal) | `ProbeGraphFactory` / `ProbeServices` / `ProbePlatformHolder` |

Any other `ZDebug*`-prefixed symbol not listed follows the same pattern: drop the `Z`/`Debug`
prefix, keep the rest of the name (e.g. `ZDebugNotificationBridge`-style consumer glue becomes
sample-app-local code, not part of the library).

Package namespace root stays `com.dev.probe` (the wizard's default), not `dev.probe` — confirmed
choice despite the initially-discussed `dev.probe` root.

## Dependency & version plan

Pin Probe's `gradle/libs.versions.toml` to `zebpay_multiplatform`'s current values for every
shared version, since that's the actual consumer this library is being built for:

| | zebpay_multiplatform | Probe (wizard default) | Action |
|---|---|---|---|
| Kotlin | 2.4.0 | 2.4.20 | downgrade |
| Compose Multiplatform | 1.11.1 | 1.12.0 | downgrade |
| AGP | 9.2.1 | 9.1.1 | upgrade |
| compileSdk | 37 | 37 | no change |
| minSdk | 28 | 24 | raise |

Add the following to Probe's version catalog (new entries, versions copied verbatim from
`zebpay_multiplatform`'s `gradle/libs.versions.toml`), needed because `probe-runtime` uses them:

- Ktor: `ktor-client-core`, `ktor-server-core`, `ktor-server-cio`, `ktor-server-websockets`,
  `ktor-server-cors`, `ktor-server-content-negotiation`, `ktor-client-kotlinx-json`,
  `ktor-client-mock`, `ktor-client-engine-cio`
- `koin-core`
- `kotlinx-datetime`, `kotlinx-serialization-json`
- `moko-permission`, `moko-permission-camera`, `moko-permission-location`,
  `moko-permission-notifications`
- `androidx-room-runtime` + `androidx-room-compiler` (KSP), `sqlite-bundled`
- `androidx-datastore-preferences`
- `androidx-startup-runtime`
- `androidx-lifecycle-runtimeCompose` (already present via `androidx-lifecycle`, confirm version
  matches)
- `material-icons-extended`, `ui-backhandler`
- `robolectric`, `junit` (test-only, `androidHostTest`), `androidx-core`

Add the following plugins to Probe's root `build.gradle.kts` / version catalog (currently
missing): `kotlinSerialization`, `room`, `ksp`, `androidLint`.

## Migration mechanics

1. `git init` in `/Users/dianapps/StudioProjects/Probe` — first commit is the untouched wizard
   scaffold, so the port itself is a reviewable diff, not folded into project setup noise.
2. Copy `zdebug-api/src/**` → `probe-api/src/**` and `zdevtools/src/**` → `probe-runtime/src/**`
   verbatim, including `androidHostTest`/`commonTest`/`commonMain` test sources — behavior must
   carry over unchanged, not just be re-derived.
3. Mechanical rename pass across all copied files: packages and symbols per the rename table
   above. No logic changes beyond what the rename forces (e.g. import paths).
4. Port `build.gradle.kts` for both modules and `zdevtools/src/androidMain/AndroidManifest.xml`
   (the App Startup `<provider>` entry), renamed accordingly.
5. `zdevtools/docs/runtime-state-capture-spec.md` and the plan docs under
   `docs/superpowers/plans/` in `zebpay_multiplatform` are **not** ported — they document
   zebpay-internal planning history for work explicitly out of scope here (see Non-goals).
6. Confirm no zebpay-specific coupling leaks into library code: `ZBuildConfig`/`ZEnvConfig` are
   only referenced from *consumer* wiring in `zebpay_multiplatform`
   (`androidApp/.../AppModules.kt`, `zebpayApp/.../ZDebugIosBootstrap.kt`), never from inside
   `zdevtools`/`zdebug-api` itself — confirmed during the original wiring work. The library code
   ports cleanly; only Probe's own sample app needs a trivial local `isEnabled = { true }`-style
   config, no zebpay-config equivalent needed.
7. Update `androidApp`/`iosApp` from wizard-default Greeting screens into a real integration
   sample: Android self-installs `probe-runtime` via its App Startup `Initializer` (same
   mechanism as production); iOS calls the bootstrap function from `iOSApp.swift`'s `init()`
   (matching the timing fix already validated in `zebpay_multiplatform`'s port — before
   `AppDelegate.didFinishLaunchingWithOptions` fires). Both expose a simple way to open the debug
   hub for manual verification.

## Verification

- `./gradlew :probe-api:compileAndroidMain :probe-runtime:compileAndroidMain` compiles clean on
  both modules.
- Existing `androidHostTest`/`commonTest` suites (renamed, not rewritten) pass unmodified in
  behavior.
- Sample Android app builds, installs, and self-registers the debug hub.
- Sample iOS app builds; manual run to confirm the bootstrap call fires at the right point in
  app lifecycle (best-effort — no CI for iOS in this pass).

## Open questions for follow-on work (not blocking this migration)

- Consumption mechanism for `zebpay_multiplatform` to depend on Probe (Maven/GitHub Packages vs.
  composite build) — deferred, per Non-goals.
- Whether/when to rebuild the archived DB/DataStore/Log capture spike on top of Probe once this
  migration lands.
