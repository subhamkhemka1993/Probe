package com.dev.probe.internal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.dev.probe.api.ProbePlatformContext
import com.dev.probe.db.ProbeDatabase
import com.dev.probe.db.getDatabaseBuilder
import com.dev.probe.db.getProbeDatabase
import com.dev.probe.prefs.createDebugPreferencesDataStore

internal actual fun createDatabase(platform: ProbePlatformContext): ProbeDatabase =
    getProbeDatabase(getDatabaseBuilder())

internal actual fun createPreferencesDataStore(platform: ProbePlatformContext): DataStore<Preferences> =
    createDebugPreferencesDataStore()
