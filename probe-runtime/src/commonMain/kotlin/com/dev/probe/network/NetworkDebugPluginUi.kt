package com.dev.probe.network

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dev.probe.Probe
import com.dev.probe.network.ui.NetworkDetailContent
import com.dev.probe.network.ui.NetworkListContent
import com.dev.probe.plugin.ProbePlugin
import com.dev.probe.session.DebugSessionManager
import com.dev.probe.session.SessionRole
import com.dev.probe.ui.SessionChipRow
import com.dev.probe.ui.primitives.ProbeDivider
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * [ProbePlugin] for the network inspector: switches between [NetworkListContent] and
 * [NetworkDetailContent] based on the currently selected [com.dev.probe.network.model.NetworkCall].
 * A [SessionChipRow] lets the user toggle between the active (this process) session and the
 * read-only previous session from the last process.
 */
internal class NetworkDebugPluginUi(private val repository: NetworkDebugRepository, private val sessionManager: DebugSessionManager) :
    ProbePlugin {
    override val id = "network"
    override val displayName = "Network"
    override val description = "Inspect HTTP traffic and choose output mode"

    @Composable
    override fun PanelContent(onClose: () -> Unit) {
        var selectedId by remember { mutableStateOf<String?>(null) }
        var searchQuery by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf(SessionRole.CURRENT) }

        val activeSession by sessionManager.activeSession().collectAsState()
        val availableSessions by sessionManager.availableSessions().collectAsState(initial = emptyList())
        val previousSession = availableSessions.find { it.role == SessionRole.PREVIOUS }
        val previousAvailable = previousSession != null

        LaunchedEffect(previousAvailable) {
            if (selectedRole == SessionRole.PREVIOUS && !previousAvailable) {
                selectedRole = SessionRole.CURRENT
            }
        }

        val viewedSessionId =
            when (selectedRole) {
                SessionRole.CURRENT -> activeSession.id
                SessionRole.PREVIOUS -> previousSession?.id
            }
        val isReadOnly = selectedRole == SessionRole.PREVIOUS

        val callsFlow =
            remember(viewedSessionId, searchQuery) {
                viewedSessionId?.let { repository.observeCalls(it, searchQuery) } ?: flowOf(emptyList())
            }
        val calls by callsFlow.collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()
        val selected = calls.find { it.id == selectedId }

        DisposableEffect(selectedId) {
            Probe.setInspectorBackHandler {
                if (selectedId != null) {
                    selectedId = null
                    true
                } else {
                    Probe.showNetworkModePicker()
                    true
                }
            }
            onDispose { Probe.setInspectorBackHandler(null) }
        }

        if (selected == null) {
            Column(modifier = Modifier.fillMaxSize()) {
                SessionChipRow(
                    selectedRole = selectedRole,
                    previousAvailable = previousAvailable,
                    onRoleSelected = { selectedRole = it },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
                ProbeDivider()
                NetworkListContent(
                    calls = calls,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onCallClick = { selectedId = it.id },
                    onClear = {
                        if (!isReadOnly) {
                            scope.launch { repository.clear(viewedSessionId ?: return@launch) }
                        }
                    },
                    onClose = onClose,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            NetworkDetailContent(
                call = selected,
                onBack = { selectedId = null },
            )
        }
    }
}
