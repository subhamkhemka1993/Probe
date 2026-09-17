package com.dev.probe.devactions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.location.LOCATION
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION

internal actual class PermissionDevActionsController internal constructor(
    private val context: Context,
    private val mokoController: PermissionsController,
) {
    actual suspend fun statusOf(id: String): PermissionState {
        val manifestPermission = id.toManifestPermission() ?: return PermissionState.Unsupported
        if (!isDeclared(manifestPermission)) return PermissionState.Unsupported
        val granted =
            ContextCompat.checkSelfPermission(context, manifestPermission) ==
                PackageManager.PERMISSION_GRANTED
        return if (granted) PermissionState.Granted else PermissionState.NotDetermined
    }

    actual suspend fun request(id: String): PermissionState {
        val manifestPermission = id.toManifestPermission() ?: return PermissionState.Unsupported
        if (!isDeclared(manifestPermission)) return PermissionState.Unsupported
        val mokoPermission = id.toMokoPermission() ?: return statusOf(id)
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
        val intent =
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Manifest declaration gate — moko can't distinguish "not declared" from "denied". */
    private fun isDeclared(manifestPermission: String): Boolean = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            ?.contains(manifestPermission)
    }.getOrNull() ?: false

    private fun String.toManifestPermission(): String? = when (this) {
        KnownPermission.Notifications.id -> Manifest.permission.POST_NOTIFICATIONS
        KnownPermission.Camera.id -> Manifest.permission.CAMERA
        KnownPermission.Location.id -> Manifest.permission.ACCESS_FINE_LOCATION
        else -> null
    }

    private fun String.toMokoPermission(): Permission? = when (this) {
        KnownPermission.Notifications.id -> Permission.REMOTE_NOTIFICATION
        KnownPermission.Camera.id -> Permission.CAMERA
        KnownPermission.Location.id -> Permission.LOCATION
        else -> null
    }
}

@Composable
internal actual fun rememberPermissionDevActionsController(): PermissionDevActionsController {
    val context = LocalContext.current
    val factory = rememberPermissionsControllerFactory()
    val mokoController = remember(factory) { factory.createPermissionsController() }
    BindEffect(mokoController)
    return remember(mokoController, context) {
        PermissionDevActionsController(context = context.applicationContext, mokoController = mokoController)
    }
}
