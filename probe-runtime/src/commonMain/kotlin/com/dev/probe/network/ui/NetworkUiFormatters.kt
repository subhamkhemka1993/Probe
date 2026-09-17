@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.dev.probe.network.ui

import com.dev.probe.network.BodyPrettyPrinter
import com.dev.probe.network.model.NetworkCall
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

internal enum class NetworkStatusTone {
    Success,
    Warning,
    Error,
    Neutral,
    Pending,
}

internal data class NetworkStatusPresentation(val label: String, val tone: NetworkStatusTone)

/** Display text (pretty-printed when applicable) + raw payload for copy. */
internal data class FormattedBody(val displayText: String, val fullText: String)

internal object NetworkBodyFormatter {
    /** Max chars rendered in the Body tab; full payload remains available via Copy. */
    private const val DISPLAY_MAX_CHARS = 16_384

    fun empty(): FormattedBody {
        val placeholder = "(empty)"
        return FormattedBody(displayText = placeholder, fullText = placeholder)
    }

    fun prepare(body: String?, contentType: String? = null): FormattedBody {
        if (body.isNullOrBlank()) return empty()
        val trimmed = body.trim()
        val pretty = BodyPrettyPrinter.format(trimmed, contentType)
        val display =
            if (pretty.length > DISPLAY_MAX_CHARS) {
                buildString {
                    append(pretty.take(DISPLAY_MAX_CHARS))
                    append("\n\n… truncated (")
                    append(trimmed.length)
                    append(" chars total — use Copy for full body)")
                }
            } else {
                pretty
            }
        return FormattedBody(displayText = display, fullText = trimmed)
    }

    fun formatForCopy(body: String, contentType: String? = null): String = BodyPrettyPrinter.format(body, contentType)
}

internal fun NetworkCall.statusPresentation(): NetworkStatusPresentation {
    val status = responseStatus
    return when {
        error != null -> NetworkStatusPresentation("ERR", NetworkStatusTone.Error)
        !isComplete || status == null -> NetworkStatusPresentation("…", NetworkStatusTone.Pending)
        status in 200..299 -> NetworkStatusPresentation(status.toString(), NetworkStatusTone.Success)
        status in 400..499 -> NetworkStatusPresentation(status.toString(), NetworkStatusTone.Warning)
        status >= 500 -> NetworkStatusPresentation(status.toString(), NetworkStatusTone.Error)
        else -> NetworkStatusPresentation(status.toString(), NetworkStatusTone.Neutral)
    }
}

internal fun NetworkCall.durationLabel(): String = durationMs?.let { "${it}ms" } ?: if (isComplete) "—" else "…"

internal fun NetworkCall.responseSizeLabel(): String {
    if (!isComplete) return "…"
    val wireBytes = responseHeaders?.contentLengthBytes()
    val capturedBytes = (responseBody ?: error)?.encodeToByteArray()?.size?.toLong()
    val truncated = isResponseBodyTruncated()

    return when {
        wireBytes != null && truncated && capturedBytes != null ->
            "${formatNetworkByteSize(wireBytes)} (${formatNetworkByteSize(capturedBytes)} captured)"

        wireBytes != null -> formatNetworkByteSize(wireBytes)

        capturedBytes != null ->
            formatNetworkByteSize(capturedBytes) + if (truncated) " (truncated)" else ""

        else -> "—"
    }
}

private fun NetworkCall.isResponseBodyTruncated(): Boolean = responseBody?.contains("… [truncated at") == true

private fun Map<String, String>.contentLengthBytes(): Long? = entries
    .firstOrNull { it.key.equals("Content-Length", ignoreCase = true) }
    ?.value
    ?.toLongOrNull()
    ?.takeIf { it >= 0L }

internal fun formatNetworkByteSize(bytes: Long): String = when {
    bytes < 1_024L -> "$bytes B"
    bytes < 1_024L * 1_024L -> formatNetworkByteSizeUnit(bytes, 1_024L, "KB")
    else -> formatNetworkByteSizeUnit(bytes, 1_024L * 1_024L, "MB")
}

private fun formatNetworkByteSizeUnit(bytes: Long, unit: Long, suffix: String): String {
    val value = bytes.toDouble() / unit.toDouble()
    val rounded = ((value * 10).toLong()) / 10.0
    val text =
        if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    return "$text $suffix"
}

internal fun NetworkCall.overviewStatusLabel(): String = when {
    responseStatus != null -> responseStatus.toString()
    error != null -> "Failed: $error"
    !isComplete -> "Pending…"
    else -> "—"
}

internal fun formatNetworkTimestamp(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    val second = local.second.toString().padStart(2, '0')
    val month =
        local.month.number
            .toString()
            .padStart(2, '0')
    val day = local.dayOfMonth.toString().padStart(2, '0')
    return "$day/$month/${local.year} $hour:$minute:$second"
}

internal fun formatRelativeTimestamp(epochMillis: Long): String {
    val diffMs = Clock.System.now().toEpochMilliseconds() - epochMillis
    return when {
        diffMs < 5_000 -> "just now"
        diffMs < 60_000 -> "${diffMs / 1_000}s ago"
        diffMs < 3_600_000 -> "${diffMs / 60_000}m ago"
        diffMs < 86_400_000 -> "${diffMs / 3_600_000}h ago"
        else -> formatNetworkTimestamp(epochMillis)
    }
}
