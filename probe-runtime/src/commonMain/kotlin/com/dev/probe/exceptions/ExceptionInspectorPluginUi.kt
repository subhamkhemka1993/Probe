package com.dev.probe.exceptions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.probe.plugin.ProbePlugin
import com.dev.probe.session.DebugSessionManager
import com.dev.probe.session.SessionRole
import com.dev.probe.theme.LocalProbeColors
import com.dev.probe.theme.LocalProbeTypography
import com.dev.probe.ui.SessionChipRow
import com.dev.probe.ui.primitives.ProbeDivider
import com.dev.probe.ui.primitives.ProbeListRow

/**
 * [ProbePlugin] for the Exceptions & Crashes inspector: renders [store]'s persisted entries
 * (fatal + non-fatal), each expandable to its full stack trace — unlike the Log inspector's
 * one-line summary, since the trace is the entire point of this panel. Uses the same
 * [SessionChipRow] the network inspector uses to switch between the active (current) session and
 * the read-only previous one, since [CrashEntry.sessionId] is stamped with the same session ids
 * [DebugSessionManager] already tracks.
 */
internal class ExceptionInspectorPluginUi(private val store: CrashLogStore, private val sessionManager: DebugSessionManager) :
    ProbePlugin {
    override val id = "exceptions"
    override val displayName = "Exceptions"
    override val description = "Captured crashes and reported exceptions"

    @Composable
    override fun PanelContent(onClose: () -> Unit) {
        val allEntries by store.entries.collectAsState()
        val activeSession by sessionManager.activeSession().collectAsState()
        val availableSessions by sessionManager.availableSessions().collectAsState(initial = emptyList())
        val previousSession = availableSessions.find { it.role == SessionRole.PREVIOUS }
        val previousAvailable = previousSession != null
        var selectedRole by remember { mutableStateOf(SessionRole.CURRENT) }
        var expandedId by remember { mutableStateOf<Long?>(null) }

        LaunchedEffect(previousAvailable) {
            if (selectedRole == SessionRole.PREVIOUS && !previousAvailable) {
                selectedRole = SessionRole.CURRENT
            }
        }

        val entries = visibleCrashEntries(allEntries, selectedRole, activeSession.id, previousSession?.id)

        Column(modifier = Modifier.fillMaxSize()) {
            SessionChipRow(
                selectedRole = selectedRole,
                previousAvailable = previousAvailable,
                onRoleSelected = { selectedRole = it },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
            ProbeDivider()

            if (entries.isEmpty()) {
                Text(
                    text = "No exceptions captured yet.",
                    style = LocalProbeTypography.current.bodyMedium,
                    color = LocalProbeColors.current.textSecondary,
                    modifier = Modifier.padding(16.dp),
                )
                return@Column
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(entries.sortedByDescending { it.timestampMillis }, key = { it.id }) { entry ->
                    ProbeListRow(
                        onClick = { expandedId = if (expandedId == entry.id) null else entry.id },
                        leftContent = {
                            Column {
                                Text(
                                    text = "${if (entry.isFatal) "FATAL" else "CAUGHT"} · ${entry.exceptionClassName}",
                                    style = LocalProbeTypography.current.bodyMedium,
                                    color = LocalProbeColors.current.textPrimary,
                                )
                                Text(
                                    text = entry.message ?: "(no message)",
                                    style = LocalProbeTypography.current.bodySmall,
                                    color = LocalProbeColors.current.textSecondary,
                                )
                                if (expandedId == entry.id) {
                                    Text(
                                        text = entry.stackTrace,
                                        style = LocalProbeTypography.current.bodySmall,
                                        color = LocalProbeColors.current.textSecondary,
                                    )
                                }
                            }
                        },
                    )
                    ProbeDivider()
                }
            }
        }
    }
}

/**
 * Extracted from [ExceptionInspectorPluginUi.PanelContent] so this filtering logic — which
 * session's entries are visible, and the pending-entry special case — is unit-testable without
 * Compose UI testing infrastructure (no other plugin in this codebase has that set up).
 *
 * An entry stamped with [DebugSessionManager.PENDING_SESSION_ID] (a crash before the first
 * `ensureInitialSession()` resolved, before [CrashLogStore.retagPending] ran) is shown under
 * CURRENT rather than dropped — it isn't a real previous-process session, so it has nowhere else
 * to render.
 */
internal fun visibleCrashEntries(
    allEntries: List<CrashEntry>,
    selectedRole: SessionRole,
    activeSessionId: String,
    previousSessionId: String?,
): List<CrashEntry> {
    val viewedSessionId = when (selectedRole) {
        SessionRole.CURRENT -> activeSessionId
        SessionRole.PREVIOUS -> previousSessionId
    }
    return allEntries.filter {
        it.sessionId == viewedSessionId ||
            (selectedRole == SessionRole.CURRENT && it.sessionId == DebugSessionManager.PENDING_SESSION_ID)
    }
}
