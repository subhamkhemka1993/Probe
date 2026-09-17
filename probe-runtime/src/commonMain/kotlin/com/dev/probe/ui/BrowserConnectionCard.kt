package com.dev.probe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dev.probe.browser.BrowserConnectionInfo
import com.dev.probe.network.ui.NetworkStatusTone
import com.dev.probe.preview.ProbeBackgroundPreviewContainer
import com.dev.probe.preview.ThemePreviews
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.theme.medium
import com.dev.probe.theme.semiBold
import com.dev.probe.ui.primitives.ProbePrimaryButton
import com.dev.probe.ui.primitives.ProbeStatusBadge

private val CardShape = RoundedCornerShape(12.dp)
private val LabelWidth = 72.dp

/**
 * Loopback host a developer's desktop browser reaches after `adb forward`-ing the emulator's
 * debug-server port — independent of [BrowserConnectionInfo.Running.simulatorUrl], which is only
 * populated on an actual iOS Simulator.
 */
private const val EMULATOR_DESKTOP_LOOPBACK_HOST = "127.0.0.1"

@Composable
internal fun BrowserConnectionCard(
    connectionInfo: BrowserConnectionInfo,
    onCopyLink: (String) -> Unit,
    onCopyToken: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalProbeColors.current

    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (connectionInfo) {
            BrowserConnectionInfo.Stopped ->
                BrowserConnectionMessage(
                    title = "Starting server",
                    subtitle = "The LAN inspector will appear here once the server is ready.",
                    tone = NetworkStatusTone.Neutral,
                )

            is BrowserConnectionInfo.Error ->
                BrowserConnectionMessage(
                    title = "Server error",
                    subtitle = connectionInfo.message,
                    tone = NetworkStatusTone.Error,
                )

            is BrowserConnectionInfo.Running ->
                BrowserConnectionRunning(
                    connectionInfo = connectionInfo,
                    onCopyLink = onCopyLink,
                    onCopyToken = onCopyToken,
                )
        }
    }
}

@Composable
private fun BrowserConnectionMessage(title: String, subtitle: String, tone: NetworkStatusTone) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProbeStatusBadge(text = title, tone = tone)
        Text(
            text = subtitle,
            style = typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BrowserConnectionRunning(
    connectionInfo: BrowserConnectionInfo.Running,
    onCopyLink: (String) -> Unit,
    onCopyToken: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProbeStatusBadge(
            text = "Running on port ${connectionInfo.port} (LAN)",
            tone = NetworkStatusTone.Success,
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            BrowserUrlRow(label = "Wi\u2011Fi", url = connectionInfo.wifiUrl)

            if (connectionInfo.emulatorUrl != null) {
                EmulatorHelpAccordion(
                    adbCommand = "adb forward tcp:${connectionInfo.port} tcp:${connectionInfo.port}",
                    desktopUrl = emulatorDesktopUrl(connectionInfo.port, connectionInfo.token),
                )
            } else if (connectionInfo.showsSimulatorRow()) {
                BrowserUrlRow(label = "Simulator", url = checkNotNull(connectionInfo.simulatorUrl))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProbePrimaryButton(
                title = "Copy link",
                onClick = { onCopyLink(connectionInfo.copyLinkUrl()) },
                modifier = Modifier.weight(1f),
            )
            ProbePrimaryButton(
                title = "Copy token",
                onClick = { onCopyToken(connectionInfo.token) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * "Copy link" always targets the Wi‑Fi URL — the emulator/simulator URLs are footnotes surfaced
 * only via [EmulatorHelpAccordion], never the default copy target (Spider LAN parity).
 */
internal fun BrowserConnectionInfo.Running.copyLinkUrl(): String = wifiUrl

/**
 * The desktop-facing loopback URL shown inside [EmulatorHelpAccordion], reachable once a developer
 * `adb forward`s the emulator's debug-server [port]. Deliberately independent of
 * [BrowserConnectionInfo.Running.simulatorUrl] — that field only reports non-null on an actual iOS
 * Simulator, whereas this URL must stay available whenever [BrowserConnectionInfo.Running.emulatorUrl]
 * is non-null (i.e. on an Android emulator, where `isSimulator()` is always `false`).
 */
internal fun emulatorDesktopUrl(port: Int, token: String): String = "http://$EMULATOR_DESKTOP_LOOPBACK_HOST:$port/?token=$token"

/**
 * The "Simulator" URL row is only relevant when there's no emulator accordion already covering
 * desktop access, and only when [BrowserConnectionInfo.Running.simulatorUrl] is populated (i.e. an
 * actual iOS Simulator). Both conditions are checked explicitly rather than relying on the two
 * platforms' current mutual exclusivity ([BrowserConnectionInfo.Running.emulatorUrl] and
 * [BrowserConnectionInfo.Running.simulatorUrl] happen to never both be non-null today, since
 * `isEmulator()`/`isSimulator()` are hardcoded `false` on the platform where the other applies).
 */
internal fun BrowserConnectionInfo.Running.showsSimulatorRow(): Boolean = emulatorUrl == null && simulatorUrl != null

@Composable
private fun EmulatorHelpAccordion(adbCommand: String, desktopUrl: String) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (expanded) "Collapse emulator desktop access help" else "Expand emulator desktop access help",
                tint = colors.textSecondary,
                modifier = Modifier.width(20.dp),
            )
            Text(
                text = "Emulator desktop access",
                style = typography.bodySmall.medium(),
                color = colors.textSecondary,
            )
        }

        if (expanded) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Desktop browsers can't reach the emulator's LAN IP directly.",
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                )
                BrowserUrlRow(label = "Run", url = adbCommand)
                BrowserUrlRow(label = "Open", url = desktopUrl)
            }
        }
    }
}

@Composable
private fun BrowserUrlRow(label: String, url: String) {
    val colors = LocalProbeColors.current
    val typography = LocalProbeTypography.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = typography.bodySmall.medium(),
            color = colors.textSecondary,
            modifier = Modifier.width(LabelWidth),
        )
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(
                text = url,
                style = typography.code.semiBold(),
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@ThemePreviews
@Composable
private fun BrowserConnectionCardStoppedPreview() {
    ProbeBackgroundPreviewContainer {
        BrowserConnectionCard(
            connectionInfo = BrowserConnectionInfo.Stopped,
            onCopyLink = {},
            onCopyToken = {},
        )
    }
}

@ThemePreviews
@Composable
private fun BrowserConnectionCardDevicePreview() {
    ProbeBackgroundPreviewContainer {
        BrowserConnectionCard(
            connectionInfo =
            BrowserConnectionInfo.Running(
                port = 8765,
                token = "sample-token",
                wifiUrl = "http://192.168.1.12:8765/?token=sample-token",
                emulatorUrl = null,
                simulatorUrl = "http://127.0.0.1:8765/?token=sample-token",
            ),
            onCopyLink = {},
            onCopyToken = {},
        )
    }
}

@ThemePreviews
@Composable
private fun BrowserConnectionCardEmulatorPreview() {
    ProbeBackgroundPreviewContainer {
        BrowserConnectionCard(
            connectionInfo =
            BrowserConnectionInfo.Running(
                port = 8765,
                token = "sample-token",
                wifiUrl = "http://10.0.2.16:8765/?token=sample-token",
                emulatorUrl = "http://10.0.2.2:8765/?token=sample-token",
                simulatorUrl = "http://127.0.0.1:8765/?token=sample-token",
            ),
            onCopyLink = {},
            onCopyToken = {},
        )
    }
}
