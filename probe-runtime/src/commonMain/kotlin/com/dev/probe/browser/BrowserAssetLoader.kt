package com.dev.probe.browser

internal expect object BrowserAssetLoader {
    fun readText(relativePath: String): String
}
