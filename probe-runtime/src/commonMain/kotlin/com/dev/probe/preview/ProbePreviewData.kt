package com.dev.probe.preview

import androidx.compose.runtime.Composable
import com.dev.probe.network.model.NetworkCall
import com.dev.probe.plugin.ProbePlugin

/** Fixed timestamps so relative-time labels stay stable in Compose previews. */
private const val BASE_TS = 1_751_904_600_000L

internal object ProbePreviewData {
    internal val plugins =
        listOf(
            previewPlugin(
                id = "network",
                displayName = "Network",
                description = "Inspect HTTP traffic and choose output mode",
            ),
            previewPlugin(
                id = "flags",
                displayName = "Feature flags",
                description = "Toggle remote config overrides for local testing",
            ),
        )

    val sampleHeaders =
        mapOf(
            "Accept" to "application/json",
            "Authorization" to "Bearer eyJhbGci***",
            "Content-Type" to "application/json; charset=utf-8",
            "User-Agent" to "Zebpay/4.0.0 (Android 15; Pixel 8)",
            "X-Request-Id" to "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        )

    const val minifiedJsonBody =
        """{"status":"success","data":{"layoutVersion":42,"sections":[{"id":"hero","type":"banner"},{"id":"markets","type":"ticker"}]}}"""

    val getHomeLayout =
        NetworkCall(
            id = "call-get-home",
            timestampMillis = BASE_TS - 12_000,
            method = "GET",
            url = "https://www.zebapi.com/api/v1/home-screen/layout?country=IN&platform=android",
            host = "www.zebapi.com",
            path = "/api/v1/home-screen/layout",
            query = "country=IN&platform=android",
            requestHeaders = sampleHeaders.filterKeys { it != "Content-Type" },
            requestBody = null,
            responseStatus = 200,
            responseHeaders =
            mapOf(
                "content-type" to "application/json; charset=utf-8",
                "cache-control" to "max-age=60",
                "Content-Length" to minifiedJsonBody.encodeToByteArray().size.toString(),
            ),
            responseBody = minifiedJsonBody,
            durationMs = 193,
            error = null,
            isComplete = true,
        )

    val postLogin =
        NetworkCall(
            id = "call-post-login",
            timestampMillis = BASE_TS - 45_000,
            method = "POST",
            url = "https://www.zebapi.com/api/v2/auth/login",
            host = "www.zebapi.com",
            path = "/api/v2/auth/login",
            query = null,
            requestHeaders = sampleHeaders,
            requestBody = """{"email":"user@example.com","password":"***"}""",
            responseStatus = 201,
            responseHeaders = mapOf("content-type" to "application/json"),
            responseBody = """{"token":"***","refreshToken":"***"}""",
            durationMs = 412,
            error = null,
            isComplete = true,
        )

    val getNotFound =
        NetworkCall(
            id = "call-get-404",
            timestampMillis = BASE_TS - 90_000,
            method = "GET",
            url = "https://www.zebapi.com/api/v1/wallets/unknown-coin",
            host = "www.zebapi.com",
            path = "/api/v1/wallets/unknown-coin",
            query = null,
            requestHeaders = sampleHeaders.filterKeys { it != "Content-Type" },
            requestBody = null,
            responseStatus = 404,
            responseHeaders = mapOf("content-type" to "application/json"),
            responseBody = """{"error":"Wallet not found"}""",
            durationMs = 87,
            error = null,
            isComplete = true,
        )

    val getServerError =
        NetworkCall(
            id = "call-get-500",
            timestampMillis = BASE_TS - 120_000,
            method = "GET",
            url = "https://www.zebapi.com/api/v1/exchange/orderbook/BTC-INR",
            host = "www.zebapi.com",
            path = "/api/v1/exchange/orderbook/BTC-INR",
            query = null,
            requestHeaders = sampleHeaders.filterKeys { it != "Content-Type" },
            requestBody = null,
            responseStatus = 500,
            responseHeaders = mapOf("content-type" to "text/plain"),
            responseBody = "Internal Server Error",
            durationMs = 1_204,
            error = null,
            isComplete = true,
        )

    val pendingCall =
        NetworkCall(
            id = "call-pending",
            timestampMillis = BASE_TS - 2_000,
            method = "GET",
            url = "https://www.zebapi.com/api/v1/user/profile",
            host = "www.zebapi.com",
            path = "/api/v1/user/profile",
            query = null,
            requestHeaders = sampleHeaders.filterKeys { it != "Content-Type" },
            requestBody = null,
            responseStatus = null,
            responseHeaders = null,
            responseBody = null,
            durationMs = null,
            error = null,
            isComplete = false,
        )

    val failedCall =
        NetworkCall(
            id = "call-failed",
            timestampMillis = BASE_TS - 180_000,
            method = "POST",
            url = "https://www.zebapi.com/api/v1/quicktrade/quote",
            host = "www.zebapi.com",
            path = "/api/v1/quicktrade/quote",
            query = null,
            requestHeaders = sampleHeaders,
            requestBody = """{"pair":"BTC-INR","side":"buy","amount":"1000"}""",
            responseStatus = null,
            responseHeaders = null,
            responseBody = null,
            durationMs = 30_000,
            error = "SocketTimeoutException: timeout",
            isComplete = true,
        )

    val networkCalls =
        listOf(
            pendingCall,
            getHomeLayout,
            postLogin,
            getNotFound,
            getServerError,
            failedCall,
        )
}

private fun previewPlugin(id: String, displayName: String, description: String = ""): ProbePlugin = object : ProbePlugin {
    override val id: String = id
    override val displayName: String = displayName
    override val description: String = description

    @Composable
    override fun PanelContent(onClose: () -> Unit) = Unit
}
