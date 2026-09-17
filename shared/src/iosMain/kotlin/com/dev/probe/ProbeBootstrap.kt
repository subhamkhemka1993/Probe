package com.dev.probe

import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.startup.installProbeTools

fun installProbeSample() {
    installProbeTools(
        config = ProbeConfig(isEnabled = { true }),
        platform = ProbePlatformContext(),
    )
}
