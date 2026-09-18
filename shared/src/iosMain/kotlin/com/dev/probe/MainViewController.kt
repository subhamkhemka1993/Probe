package com.dev.probe

import androidx.compose.ui.window.ComposeUIViewController
import com.dev.probe.api.ProbeHub
import com.dev.probe.api.ProbePlatformContext

fun MainViewController() = ComposeUIViewController {
    App(resources = sampleAppResources, onOpenHub = { ProbeHub.openHub(ProbePlatformContext()) })
}
