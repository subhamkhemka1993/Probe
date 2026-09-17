package com.dev.probe.platform

internal expect fun shareFile(
    fileName: String,
    content: String,
    mimeType: String,
)
