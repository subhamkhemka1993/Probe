package com.dev.probe.network

import com.dev.probe.ProbeCaptureLimits
import com.dev.probe.session.DebugSessionManager
import kotlinx.coroutines.CoroutineScope
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
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
