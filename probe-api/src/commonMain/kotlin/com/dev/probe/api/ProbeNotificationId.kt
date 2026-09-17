package com.dev.probe.api

/**
 * Identifier for the local notification Probe posts on iOS while capturing network traffic.
 * Shared with `AppDelegate.swift` (via the `:zebpayApp` iOS bridge) so tapping the notification
 * can be routed to [ProbeHub.openHub] instead of only foregrounding the app.
 */
const val PROBE_NOTIFICATION_ID = "probe_capture"
