package com.dev.probe.internal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.db.ProbeDatabase

internal expect fun createDatabase(platform: ProbePlatformContext): ProbeDatabase

internal expect fun createPreferencesDataStore(platform: ProbePlatformContext): DataStore<Preferences>
