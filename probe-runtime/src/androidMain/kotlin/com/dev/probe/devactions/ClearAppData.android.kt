package com.dev.probe.devactions

import android.app.ActivityManager
import com.dev.probe.api.ProbePlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual suspend fun clearAppData(ctx: ProbePlatformContext): ClearDataResult {
    val activityManager = ctx.context.getSystemService(ActivityManager::class.java)
        ?: return ClearDataResult.Failed("ActivityManager unavailable")

    return withContext(Dispatchers.IO) {
        if (activityManager.clearApplicationUserData()) {
            ClearDataResult.FullResetTriggered
        } else {
            ClearDataResult.Failed("clearApplicationUserData returned false")
        }
    }
}

internal actual val clearAppDataDisclaimer: String =
    "Clears all app storage and restarts the app. This can't be undone."
