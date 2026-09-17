package com.dev.probe.network

import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.session.DebugSessionManager
import kotlinx.coroutines.CoroutineScope

internal class NetworkDebugRepositoryTest : NetworkDebugRepositoryTestBase() {
    override fun createRepository(
        config: ProbeCaptureLimits,
        scope: CoroutineScope,
    ): NetworkDebugRepository {
        val database = createTestProbeDatabase()
        return NetworkDebugRepository(
            dao = database.networkCallDao(),
            config = config,
            scope = scope,
            sessionManager = DebugSessionManager(database.debugSessionDao(), database.networkCallDao()),
        )
    }
}
