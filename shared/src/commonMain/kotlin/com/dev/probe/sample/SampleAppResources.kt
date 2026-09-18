package com.dev.probe.sample

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.dev.probe.api.ProbeDataStoreCapture
import com.dev.probe.api.ProbeDatabaseCapture
import com.dev.probe.api.ProbePlatformContext
import io.ktor.client.HttpClient

private const val SAMPLE_DATABASE_LABEL = "Sample notes"
private const val SAMPLE_DATASTORE_LABEL = "Sample preferences"

class SampleAppResources(val database: SampleDatabase, val dataStore: DataStore<Preferences>, val httpClient: HttpClient)

/**
 * Builds this sample app's own demo resources and registers them with Probe, exactly the way a
 * real host app would: once, right where each resource is constructed, with no debug-build
 * branching needed (see [ProbeDatabaseCapture.register]/[ProbeDataStoreCapture.register]).
 */
fun installSampleResources(platform: ProbePlatformContext): SampleAppResources {
    val database = createSampleDatabase(platform)
    ProbeDatabaseCapture.register(SAMPLE_DATABASE_LABEL, database)

    val dataStore = createSampleDataStore(platform)
    ProbeDataStoreCapture.register(SAMPLE_DATASTORE_LABEL, dataStore.data)

    val httpClient = createSampleHttpClient()

    return SampleAppResources(database, dataStore, httpClient)
}
