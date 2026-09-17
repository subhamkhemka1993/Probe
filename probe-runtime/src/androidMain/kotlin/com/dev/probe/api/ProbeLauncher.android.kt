package com.dev.probe.api

import android.content.Intent
import com.dev.probe.shell.ProbeActivity

actual object ProbeLauncher {
    actual fun openHub(ctx: ProbePlatformContext) = launch(ctx, ProbeStartScreen.Hub)

    actual fun openInspector(ctx: ProbePlatformContext) = launch(ctx, ProbeStartScreen.Inspector)

    private fun launch(ctx: ProbePlatformContext, screen: ProbeStartScreen) {
        val intent =
            Intent(ctx.context, ProbeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(PROBE_START_SCREEN, screen.name)
            }
        ctx.context.startActivity(intent)
    }
}
