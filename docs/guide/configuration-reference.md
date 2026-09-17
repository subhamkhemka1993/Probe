← [Guide index](README.md)

# Configuration Reference

| Configuration | Type | Default | Description | Platform |
|---|---|---|---|---|
| `ProbeConfig.isEnabled` | `() -> Boolean` | *(required, no default)* | Gates capture, browser server start, and the sticky notifier. Evaluated dynamically. | Both |
| `ProbeConfig.themeOverride` | `ProbeThemeOverride?` | `null` | Optional debug-shell palette override. | Both |
| `ProbeThemeOverride.*` (8 color fields: `primary`, `background`, `surface`, `textPrimary`, `textSecondary`, `success`, `warning`, `error`) | `Color?` | `null` each | Any unset field falls back to probe's Material 3 light default. | Both |
| `ProbeHostCallbacks.onClearScopedData` | `(suspend () -> List<String>)?` | `null` | Host hook invoked during iOS's scoped "clear app data"; unused on Android. | iOS (invoked); declared in `commonMain` |

No other configuration surface is exposed to host apps. Internal tunables (max captured entries,
max body bytes, browser server port/TTL/bind policy) are hardcoded in `:probe-runtime` and not part of
the public configuration API.

---
← [Guide index](README.md) · Previous: [Capability reference](capability-reference.md) · Next: [Usage examples](usage-examples.md)
