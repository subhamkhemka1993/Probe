package com.dev.probe.browser

internal data class BrowserSession(
    val token: String,
    val createdAtMillis: Long,
    val expiresAtMillis: Long,
) {
    fun isValid(nowMillis: Long): Boolean = nowMillis < expiresAtMillis
}
