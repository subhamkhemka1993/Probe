package com.dev.probe.platform

import androidx.core.content.FileProvider

/**
 * Distinct [FileProvider] subclass so the manifest merger can tell `:probe-runtime`'s provider apart
 * from a host app's own `androidx.core.content.FileProvider` declaration. The merger matches
 * `<provider>` elements by `android:name`, not `android:authorities`, so reusing the stock
 * `FileProvider` class name here would conflict with any host that already declares one.
 */
internal class ProbeFileProvider : FileProvider()
