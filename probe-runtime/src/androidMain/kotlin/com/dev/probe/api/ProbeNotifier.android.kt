package com.dev.probe.api

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.dev.probe.shell.ProbeNotificationPermissionActivity

internal actual open class ProbeNotifier actual constructor(private val platformContext: ProbePlatformContext) {
    actual open fun show(state: ProbeNotifierState) {
        val context = platformContext.context
        ProbeNotificationPoster.ensureChannel(context)

        if (!hasNotificationPermission(context)) {
            if (PendingProbeNotifierState.requestPermission(state)) {
                launchPermissionRequest(context)
            }
            return
        }

        ProbeNotificationPoster.post(context, state)
    }

    actual open fun hide() {
        ProbeNotificationPoster.hide(platformContext.context)
    }

    private fun launchPermissionRequest(context: Context) {
        val intent =
            Intent(context, ProbeNotificationPermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(intent)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

internal actual fun createProbeNotifier(platform: ProbePlatformContext): ProbeNotifier = ProbeNotifier(platform)
