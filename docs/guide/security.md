← [Guide index](README.md)

# Security & Production Considerations

**Structural exclusion is Android-only, and is the host's responsibility to wire, not automatic.**
probe provides the mechanism (a tiny always-present API module plus a heavy implementation
module meant for conditional inclusion) but a host application must actually apply it — via
Gradle's `debugImplementation`, as documented in [integration-android.md](integration-android.md).
Probe itself does not ship any CI check that verifies this; a host that wants a hard structural
guarantee (rather than trusting that no future change accidentally makes `:probe-runtime` a
transitive dependency of a release build) should add its own Gradle task that inspects the
release build type's runtime classpath and fails if `:probe-runtime` is present, and run it in CI.
This is entirely host-app-authored — `:probe-runtime` provides nothing for you here beyond being
a module you can choose not to depend on.

**Header redaction is applied everywhere a call is surfaced** (in-app detail view, browser API
responses, JSON/HAR export) — `Authorization`, `Cookie`, `X-Api-Key`, `X-Auth-Token`, and
`sessiontoken` headers are replaced with `***`. This redaction list is fixed in `:probe-runtime`'s
code and not configurable by a host app; if your APIs use a differently-named sensitive header,
it will **not** be redacted.

**Request/response bodies are not redacted at all** — only headers are. A captured body
containing sensitive data (PII, tokens embedded in a JSON payload, etc.) is stored, displayed, and
exportable as-is. Hosts with sensitive body content should account for this when deciding whether
`isEnabled` should ever be true on any build a non-developer might use.

**The embedded browser server is real network exposure while active.** It binds to the LAN
(`0.0.0.0`) by default and is reachable by anything on the same network that has (or guesses) the
session token — mitigated by an 8-hour token TTL and auto-stop when the app backgrounds, but it is
still an unauthenticated-until-token-presented HTTP server running on the device while foregrounded
with browser mode selected.

**Data persistence.** Captured network calls and preferences are stored locally (Room database,
DataStore) with no encryption applied by probe itself. This data persists across app restarts
(within session-retention limits) and is only cleared by the "Clear app data" dev action, an OS
uninstall, or an OS-level data wipe — it is not automatically purged on any timer.

**Export/share hands data to the OS share sheet**, which means the exported JSON/HAR/curl content
can leave the device entirely (email, Slack, cloud drive, etc.) at the user's discretion — probe
applies the same header redaction to exports as to on-device display, but does not restrict which
share targets are available.

**What probe is responsible for vs. the host:** probe redacts a fixed list of known-sensitive
headers and gates its own runtime behavior behind `isEnabled`. The host application is responsible
for: actually wiring build-type exclusion (Android) or a sufficiently strict `isEnabled` predicate
(both platforms, especially iOS where it's the only line of defense), deciding whether its API
surface has sensitive data in bodies or unusually-named headers that need additional handling
outside probe, and deciding whether the tool should ever reach a non-developer's device at all.

---
← [Guide index](README.md) · Previous: [Usage examples](usage-examples.md) · Next: [Troubleshooting & FAQ](troubleshooting-and-faq.md)
