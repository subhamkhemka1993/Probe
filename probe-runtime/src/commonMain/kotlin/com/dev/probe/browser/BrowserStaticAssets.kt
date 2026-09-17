package com.dev.probe.browser

internal class BrowserStaticAssets(private val resourceLoader: (String) -> String = BrowserAssetLoader::readText) {
    private val index by lazy { resourceLoader("files/browser/index.html") }
    private val css by lazy { resourceLoader("files/browser/styles.css") }
    private val js by lazy { resourceLoader("files/browser/app.js") }

    fun indexHtml(): String = index

    fun stylesCss(): String = css

    fun appJs(): String = js
}
