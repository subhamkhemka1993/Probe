package com.dev.probe.browser

internal object NetworkBrowserAuth {
    fun isAuthorized(session: BrowserSession?, bearerToken: String?, queryToken: String?, nowMillis: Long): Boolean {
        val active = session ?: return false
        if (!active.isValid(nowMillis)) return false
        val fromHeader = bearerToken?.removePrefix("Bearer ")?.trim()
        return fromHeader == active.token || queryToken == active.token
    }
}
