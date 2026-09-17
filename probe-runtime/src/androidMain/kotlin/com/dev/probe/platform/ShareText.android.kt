package com.dev.probe.platform

import android.content.Intent
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.internal.ProbePlatformHolder

internal actual fun shareText(text: String) {
    val platform = ProbePlatformHolder.requirePlatform() as ProbePlatformContext
    val context = platform.context
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    val chooser =
        Intent.createChooser(intent, "Share cURL").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    context.startActivity(chooser)
}
