package com.dev.probe.browser

import com.dev.probe.internal.ProbePlatformHolder

internal actual object BrowserAssetLoader {
    actual fun readText(relativePath: String): String {
        val context = ProbePlatformHolder.requirePlatform().context
        val assetPath = "$COMPOSE_ASSET_PREFIX$relativePath"
        return context.assets.open(assetPath).bufferedReader().use { it.readText() }
    }

    private const val COMPOSE_ASSET_PREFIX = "composeResources/com.dev.probe.resources/"
}
