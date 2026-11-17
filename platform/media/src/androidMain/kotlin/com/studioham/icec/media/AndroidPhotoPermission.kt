package com.studioham.icec.media

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidPhotoPermission(
    private val activity: ComponentActivity,
) : PhotoPermission {
    // Android cannot tell "never asked" from "blocked", so remember that we asked once.
    private val prefs = activity.getSharedPreferences("photo_permission", Context.MODE_PRIVATE)

    private val permissions =
        when {
            Build.VERSION.SDK_INT >= 34 -> arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED)
            Build.VERSION.SDK_INT >= 33 -> arrayOf(READ_MEDIA_IMAGES)
            else -> arrayOf(READ_EXTERNAL_STORAGE)
        }

    private fun granted(permission: String) = activity.checkSelfPermission(permission) == PERMISSION_GRANTED

    override fun status(): PhotoAccess =
        when {
            granted(permissions[0]) -> PhotoAccess.Granted
            Build.VERSION.SDK_INT >= 34 && granted(READ_MEDIA_VISUAL_USER_SELECTED) -> PhotoAccess.Limited
            !prefs.getBoolean(KEY_ASKED, false) -> PhotoAccess.NotDetermined
            activity.shouldShowRequestPermissionRationale(permissions[0]) -> PhotoAccess.Denied
            else -> PhotoAccess.Blocked
        }

    override suspend fun request(): PhotoAccess =
        suspendCancellableCoroutine { continuation ->
            var launcher: ActivityResultLauncher<Array<String>>? = null
            launcher =
                activity.activityResultRegistry.register(KEY_ASKED, RequestMultiplePermissions()) {
                    launcher?.unregister()
                    prefs.edit().putBoolean(KEY_ASKED, true).apply()
                    continuation.resume(status())
                }
            continuation.invokeOnCancellation { launcher.unregister() }
            launcher.launch(permissions)
        }

    override fun openSettings() {
        activity.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)),
        )
    }

    private companion object {
        const val KEY_ASKED = "photo_permission_asked"
    }
}
