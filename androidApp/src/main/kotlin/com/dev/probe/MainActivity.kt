package com.dev.probe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dev.probe.api.ProbeConfig
import com.dev.probe.api.ProbeHub
import com.dev.probe.api.ProbeInstaller
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.sample.installSampleResources

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val platform = ProbePlatformContext(this)
        ProbeInstaller.install(
            config = ProbeConfig(isEnabled = { true }),
            platform = platform,
        )
        val resources = installSampleResources(platform)

        setContent {
            App(resources = resources, onOpenHub = { ProbeHub.openHub(ProbePlatformContext(this)) })
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(onOpenHub = {})
}
