package com.dev.probe.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

internal fun TextStyle.semiBold(): TextStyle = copy(fontWeight = FontWeight.SemiBold)

internal fun TextStyle.medium(): TextStyle = copy(fontWeight = FontWeight.Medium)
