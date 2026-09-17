# probe-api

The always-present half of the Probe embedded debug shell — a settable-hook API surface with no
heavy dependencies (no database, no embedded server, no UI toolkit beyond `compose.ui`'s `Color`
type). Compile this into every build variant of your app, on every platform.

## Integrating into a fresh KMP + Ktor project

1. Depend on `:probe-api` from every module that needs to call into the shell (typically your
   networking module, for `ProbeHttpCapture`, and your app-lifecycle-observer module, for
   `ProbeState`).
2. Depend on the heavy implementation module (`:probe-runtime` in this repo) only from build
   configurations where you want the tool present — e.g. Android's `debugImplementation`. Platforms
   without a build-type dependency axis (this repo's iOS target, via Kotlin/Native) instead gate
   visibility with a runtime flag passed into `ProbeConfig.isEnabled` — see below.
3. In your Ktor `HttpClientConfig` builder, call `with(ProbeHttpCapture) { installCapture() }`
   (or the extension-function form `installCapture()` inside an `HttpClientConfig<*>` receiver).
   Before the heavy module's `initialize()` runs, this is a safe no-op.
4. In your app's process-lifetime lifecycle observer, call `ProbeState.onAppBackgrounded()` /
   `ProbeState.onAppForegrounded()` on the equivalent lifecycle events. Same no-op-until-installed
   contract as `ProbeHttpCapture`.
5. Wherever you construct the heavy module's runtime, pass a `ProbeConfig(isEnabled = { ... })`
   with your own predicate — never hardcode a specific host app's flag names inside this module or
   the heavy implementation module; keep that logic in your own integration/glue code.
6. To add a new kind of inspector (prefs, DataStore, DB, ...), implement `ProbeInspector` (or, if
   it needs UI, the heavy module's Compose-based `ProbePlugin`, which extends it) and register it
   wherever the heavy module assembles its plugin list — no changes to `:probe-api` required.

See [`../docs/guide/README.md`](../docs/guide/README.md) for the full framework guide (architecture,
per-platform integration, capability/configuration reference, security model, troubleshooting).
