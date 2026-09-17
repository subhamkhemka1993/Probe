package com.dev.probe.shell

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.dev.probe.api.PendingProbeNotifierState
import com.dev.probe.api.ProbeNotificationPoster

/**
 * Transparent, non-exported activity that requests `POST_NOTIFICATIONS` (Android 13+) on behalf
 * of [com.dev.probe.api.ProbeNotifier], which cannot request permissions itself since it
 * isn't an activity/fragment. Posts the pending notification if granted, then finishes — never
 * renders any UI beyond the system permission dialog.
 *
 * Must stay public (not `internal`) so the merged manifest can resolve
 * `.shell.ProbeNotificationPermissionActivity`, matching [ProbeActivity].
 */
class ProbeNotificationPermissionActivity : ComponentActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) postPendingNotification()
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasPermission()) {
            postPendingNotification()
            finish()
            return
        }
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Only called once onCreate has already confirmed SDK_INT >= TIRAMISU; checkSelfPermission
    // itself is safe to call with this constant on any API level regardless.
    @SuppressLint("InlinedApi")
    private fun hasPermission(): Boolean = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun postPendingNotification() {
        val state = PendingProbeNotifierState.consume() ?: return
        ProbeNotificationPoster.post(applicationContext, state)
    }
}
