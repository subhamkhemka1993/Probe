# probe-runtime

The heavy implementation half of the Probe embedded debug shell — the network inspector, embedded
browser server, Room database, DataStore-backed preferences, session manager, dev actions, and
the Compose UI shell. Depend on this only from build configurations where you want the tool
present (e.g. Android's `debugImplementation`, or iOS's `iosMain` gated by `ProbeConfig.isEnabled`
at runtime).

The full framework guide — architecture, integration steps, per-platform wiring, capability
reference, security model, troubleshooting/FAQ, extensibility, and development notes — lives in
**[`../docs/guide/README.md`](../docs/guide/README.md)**, split into one file per topic so each
piece stays easy to keep in sync with the source it documents.

For the always-present, dependency-light counterpart module, see [`../probe-api/README.md`](../probe-api/README.md).
