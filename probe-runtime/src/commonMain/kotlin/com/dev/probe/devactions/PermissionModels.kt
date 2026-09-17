package com.dev.probe.devactions

import androidx.compose.runtime.Composable

/** Coarse-grained state of a single platform permission, as reported by the OS. */
internal enum class PermissionState {
    Granted,
    Denied,
    NotDetermined,
    Unsupported,
}

/** One row in the Permissions dev panel. [id] matches a [KnownPermission.id]. */
internal data class PermissionRow(
    val id: String,
    val label: String,
    val state: PermissionState,
)

/**
 * Permissions surfaced by the dev panel. Only host-declared permissions (manifest / Info.plist)
 * resolve to anything other than [PermissionState.Unsupported] — see platform actuals.
 */
internal enum class KnownPermission(val id: String, val label: String) {
    Notifications(id = "notifications", label = "Notifications"),
    Camera(id = "camera", label = "Camera"),
    Location(id = "location", label = "Location"),
}

/**
 * Cross-platform permission status/request for the Permissions dev panel — backed by
 * moko-permissions on both platforms, gated by whether the host app declares the permission.
 */
internal expect class PermissionDevActionsController {
    /** Returns the current status without prompting the user. */
    suspend fun statusOf(id: String): PermissionState

    /** Prompts the system permission dialog for [id] and returns the resolved status. */
    suspend fun request(id: String): PermissionState

    /** Opens the host app's system settings screen. */
    fun openAppSettings()
}

/** Creates and remembers a [PermissionDevActionsController] bound to the current composition. */
@Composable
internal expect fun rememberPermissionDevActionsController(): PermissionDevActionsController
