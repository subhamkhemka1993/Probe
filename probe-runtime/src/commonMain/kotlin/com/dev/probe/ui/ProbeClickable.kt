package com.dev.probe.ui

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

internal fun Modifier.probeClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    if (!enabled) return this
    return Modifier.clickable(role = Role.Button, onClick = onClick)
}
