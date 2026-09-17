package com.dev.probe.platform

import android.content.Intent
import androidx.core.content.FileProvider
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.internal.ProbePlatformHolder
import java.io.File

private const val PROBE_EXPORT_DIR_NAME = "probe_exports"

internal actual fun shareFile(fileName: String, content: String, mimeType: String) {
    val platform = ProbePlatformHolder.requirePlatform() as ProbePlatformContext
    val context = platform.context

    val exportDir = File(context.cacheDir, PROBE_EXPORT_DIR_NAME).apply { mkdirs() }
    val file = File(exportDir, fileName)
    file.writeText(content)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.probe.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(intent, "Export session").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
