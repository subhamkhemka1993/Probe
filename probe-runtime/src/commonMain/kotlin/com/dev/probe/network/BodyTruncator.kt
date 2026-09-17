package com.dev.probe.network

import kotlin.jvm.JvmName

private const val UTF8_CONTINUATION_BYTE_MASK = 0xC0
private const val UTF8_CONTINUATION_BYTE_TAG = 0x80

/**
 * Truncates by UTF-8 byte length, not char length, so a multibyte-heavy body (emoji, CJK) can't
 * exceed [maxBytes]. Backs off from the cut point while the byte has the `10` high-bit pattern of
 * a UTF-8 continuation byte, so [decodeToString] never splits a multibyte character.
 */
internal fun truncateBody(
    body: String,
    maxBytes: Int,
): String {
    val bytes = body.encodeToByteArray()
    if (bytes.size <= maxBytes) return body
    var end = maxBytes
    while (end > 0 && (bytes[end].toInt() and UTF8_CONTINUATION_BYTE_MASK) == UTF8_CONTINUATION_BYTE_TAG) end--
    return bytes.decodeToString(0, end) + "… [truncated at ${maxBytes / 1000}KB]"
}

@JvmName("truncateBodyOrNull")
internal fun truncateBody(
    body: String?,
    maxBytes: Int,
): String? = body?.let { truncateBody(it, maxBytes) }
