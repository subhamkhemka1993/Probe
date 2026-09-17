package com.dev.probe.api

/**
 * Identifier for the local notification Probe posts on iOS while capturing network traffic.
 * The host app's notification bridge reads this to recognize a tap on that notification, so it
 * can be routed to [ProbeHub.openHub] instead of only foregrounding the app.
 */
const val PROBE_NOTIFICATION_ID = "probe_capture"
