← [Guide index](README.md)

# Extensibility

probe supports adding new inspectors via two related interfaces, both rooted in `:probe-api`:

- **`ProbeInspector`** — the minimal marker contract (`val id: String`, `val label: String`),
  with no Compose dependency, for a future headless inspector.
- **`ProbePlugin`** (in `:probe-runtime`, extends `ProbeInspector`) — adds `displayName`,
  `description`, and a `@Composable fun PanelContent(onClose: () -> Unit)`. This is what the
  built-in network inspector implements (`NetworkDebugPluginUi`).

To add a new plugin:

```kotlin
internal class MyInspectorPluginUi(/* your dependencies */) : ProbePlugin {
    override val id = "my-inspector"
    override val displayName = "My Inspector"
    override val description = "Inspect my thing"

    @Composable
    override fun PanelContent(onClose: () -> Unit) {
        // your Compose UI
    }
}
```

Then add an instance of it to the list `ProbeGraphFactory.create()` builds and passes into
`ProbeServices.plugins` — this list is what populates the debug hub's plugin section and what
`Probe.install(plugins = ...)` registers. There is no dynamic/runtime registration mechanism;
the plugin list is assembled once, at graph-construction time, in `:probe-runtime`'s own source.

Registering a plugin requires editing `:probe-runtime` source directly (there's no host-side
registration hook exposed) — so this extension point is for extending probe itself, not for a
host app to bolt on inspectors without touching this module's code.

The other extension point available to host apps without touching `:probe-runtime` source is
`ProbeHostCallbacks.onClearScopedData`, described in [configuration-reference.md](configuration-reference.md).

---
← [Guide index](README.md) · Previous: [Troubleshooting & FAQ](troubleshooting-and-faq.md) · Next: [Development](development.md)
