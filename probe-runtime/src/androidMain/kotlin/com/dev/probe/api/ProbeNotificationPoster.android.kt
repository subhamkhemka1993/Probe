package com.dev.probe.api

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.dev.probe.NetworkOutputMode
import com.dev.probe.shell.ProbeActivity

/**
 * Builds and posts the sticky Probe notification. Shared by [ProbeNotifier] (permission
 * already granted) and `ProbeNotificationPermissionActivity` (permission just granted), so both
 * entry points render an identical notification.
 */
internal object ProbeNotificationPoster {
    private const val CHANNEL_ID = "probe_capture"
    private const val NOTIFICATION_ID = 0x7DEB

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Probe capture", NotificationManager.IMPORTANCE_LOW),
        )
    }

    fun post(context: Context, state: ProbeNotifierState) {
        ensureChannel(context)
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(contentTitle(state))
                .setContentText(contentText(state))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(tapPendingIntent(context, ProbeStartScreen.Inspector))
                .addAction(0, "Hub", tapPendingIntent(context, ProbeStartScreen.Hub))
                .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Callers already check POST_NOTIFICATIONS before calling post(); this only
            // guards against the permission being revoked in the gap between that check
            // and this call.
        }
    }

    fun hide(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun tapPendingIntent(context: Context, screen: ProbeStartScreen): PendingIntent {
        val intent =
            Intent(context, ProbeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(PROBE_START_SCREEN, screen.name)
            }
        return PendingIntent.getActivity(
            context,
            screen.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun contentTitle(state: ProbeNotifierState): String =
        if (state.outputMode == NetworkOutputMode.BROWSER && state.browserUrl != null) {
            "Probe \u2022 Browser mode"
        } else {
            "Probe"
        }

    private fun contentText(state: ProbeNotifierState): String {
        val statusSuffix = state.lastStatusCode?.let { " \u2022 last: $it" }.orEmpty()
        return "${state.requestCount} requests$statusSuffix"
    }
}

/**
 * Holds the most recent [ProbeNotifierState] across the gap between [ProbeNotifier.show]
 * detecting a missing `POST_NOTIFICATIONS` permission and the permission-request activity
 * resolving it, since a notification can't be posted without the permission in between.
 *
 * [requestPermission] returns `true` only the first time it's called while permission is
 * missing, so a burst of `show()` calls (debounced capture updates) triggers the system
 * permission prompt once rather than repeatedly stealing focus.
 */
internal object PendingProbeNotifierState {
    @Volatile
    private var state: ProbeNotifierState? = null

    @Volatile
    private var permissionRequested = false

    fun requestPermission(latestState: ProbeNotifierState): Boolean {
        state = latestState
        if (permissionRequested) return false
        permissionRequested = true
        return true
    }

    fun consume(): ProbeNotifierState? = state.also { state = null }
}
