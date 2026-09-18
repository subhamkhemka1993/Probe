package com.dev.probe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dev.probe.sample.SampleAppResources
import com.dev.probe.sample.SampleLogger
import com.dev.probe.sample.SampleNoteEntity
import com.dev.probe.sample.fireSampleRequest
import com.dev.probe.sample.writeSampleValue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * The sample app's whole UI: one button per Probe capability, so opening the hub always has
 * real, freshly captured data to show — see [com.dev.probe.sample.installSampleResources].
 */
@OptIn(ExperimentalTime::class)
@Composable
@Preview
fun App(resources: SampleAppResources? = null, onOpenHub: () -> Unit = {}) {
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().safeContentPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            Button(onClick = {
                scope.launch {
                    // Caught, not propagated: a demo request failing (no network, DNS, etc.) must
                    // not crash the whole sample app — the failure is still visible in the Network
                    // inspector itself, which is the point of this button either way. Rethrows
                    // CancellationException rather than swallowing it, so leaving this screen
                    // mid-request still cancels cleanly.
                    try {
                        resources?.httpClient?.fireSampleRequest()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        SampleLogger.i("SampleApp", "Network call failed: ${e.message}")
                    }
                }
            }) {
                Text("Make a network call")
            }
            Button(onClick = {
                scope.launch {
                    resources?.database?.sampleNoteDao()?.insert(
                        SampleNoteEntity(
                            text = "Note at ${Clock.System.now()}",
                            createdAtMillis = Clock.System.now().toEpochMilliseconds(),
                        ),
                    )
                }
            }) {
                Text("Write to sample database")
            }
            Button(onClick = { scope.launch { resources?.dataStore?.writeSampleValue("Saved at ${Clock.System.now()}") } }) {
                Text("Write to sample DataStore")
            }
            Button(onClick = { SampleLogger.i("SampleApp", "Button tapped at ${Clock.System.now()}") }) {
                Text("Emit a sample log line")
            }
            Button(onClick = onOpenHub) {
                Text("Open Probe Hub")
            }
        }
    }
}
