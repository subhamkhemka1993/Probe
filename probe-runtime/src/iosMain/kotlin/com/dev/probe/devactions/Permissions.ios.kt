package com.dev.probe.devactions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.location.LOCATION
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

internal actual class PermissionDevActionsController internal constructor(
    private val mokoController: PermissionsController,
) {
    actual suspend fun statusOf(id: String): PermissionState {
        if (!isDeclared(id)) return PermissionState.Unsupported
        val mokoPermission = id.toMokoPermission() ?: return PermissionState.Unsupported
        return try {
            if (mokoController.isPermissionGranted(mokoPermission)) {
                PermissionState.Granted
            } else {
                PermissionState.NotDetermined
            }
        } catch (_: Exception) {
            PermissionState.NotDetermined
        }
    }

    actual suspend fun request(id: String): PermissionState {
        if (!isDeclared(id)) return PermissionState.Unsupported
        val mokoPermission = id.toMokoPermission() ?: return PermissionState.Unsupported
        return try {
            if (!mokoController.isPermissionGranted(mokoPermission)) {
                mokoController.providePermission(mokoPermission)
            }
            PermissionState.Granted
        } catch (_: DeniedAlwaysException) {
            PermissionState.Denied
        } catch (_: DeniedException) {
            PermissionState.Denied
        } catch (_: Exception) {
            PermissionState.Denied
        }
    }

    actual fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url)
    }

    /** Info.plist declaration gate. Notifications needs no usage-description key. */
    private fun isDeclared(id: String): Boolean {
        val infoPlistKey = id.toInfoPlistKey() ?: return true
        return NSBundle.mainBundle.objectForInfoDictionaryKey(infoPlistKey) != null
    }

    private fun String.toInfoPlistKey(): String? =
        when (this) {
            KnownPermission.Camera.id -> "NSCameraUsageDescription"
            KnownPermission.Location.id -> "NSLocationWhenInUseUsageDescription"
            else -> null
        }

    private fun String.toMokoPermission(): Permission? =
        when (this) {
            KnownPermission.Notifications.id -> Permission.REMOTE_NOTIFICATION
            KnownPermission.Camera.id -> Permission.CAMERA
            KnownPermission.Location.id -> Permission.LOCATION
            else -> null
        }
}

@Composable
internal actual fun rememberPermissionDevActionsController(): PermissionDevActionsController {
    val factory = rememberPermissionsControllerFactory()
    val mokoController = remember(factory) { factory.createPermissionsController() }
    BindEffect(mokoController)
    return remember(mokoController) { PermissionDevActionsController(mokoController) }
}
