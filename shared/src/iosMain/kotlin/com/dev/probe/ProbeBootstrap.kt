package com.dev.probe

import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.sample.SampleAppResources
import com.dev.probe.sample.installSampleResources
import com.dev.probe.startup.installProbeTools

/** Read by [MainViewController] — set once, from [installProbeSample], before Compose starts. */
internal var sampleAppResources: SampleAppResources? = null

fun installProbeSample() {
    val platform = ProbePlatformContext()
    installProbeTools(
        config = ProbeConfig(isEnabled = { true }),
        platform = platform,
    )
    sampleAppResources = installSampleResources(platform)
}
