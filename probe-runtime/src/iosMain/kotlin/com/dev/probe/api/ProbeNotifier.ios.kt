package com.dev.probe.api

import com.dev.probe.NetworkOutputMode
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionProvisional
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter

internal actual open class ProbeNotifier actual constructor(
    private val platformContext: ProbePlatformContext,
) {
    private var authorizationRequested = false

    actual open fun show(state: ProbeNotifierState) {
        requestAuthorizationIfNeeded()
        val content = UNMutableNotificationContent().apply {
            setTitle(contentTitle(state))
            setBody(contentText(state))
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = PROBE_NOTIFICATION_ID,
            content = content,
            trigger = null,
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request, withCompletionHandler = { _ -> })
    }

    actual open fun hide() {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removePendingNotificationRequestsWithIdentifiers(listOf(PROBE_NOTIFICATION_ID))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(PROBE_NOTIFICATION_ID))
    }

    /**
     * Requests alert/sound authorization, plus [UNAuthorizationOptionProvisional] so notifications
     * deliver quietly (no permission dialog) — this must never pre-empt the app's own onboarding
     * permission prompt (FCM/MoEngage).
     */
    private fun requestAuthorizationIfNeeded() {
        if (authorizationRequested) return
        authorizationRequested = true
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionProvisional,
            completionHandler = { _, _ -> },
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

internal actual fun createProbeNotifier(platform: ProbePlatformContext): ProbeNotifier =
    ProbeNotifier(platform)
