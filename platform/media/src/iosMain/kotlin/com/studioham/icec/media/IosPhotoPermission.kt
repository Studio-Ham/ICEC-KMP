package com.studioham.icec.media

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import kotlin.coroutines.resume

class IosPhotoPermission : PhotoPermission {
    override fun status(): PhotoAccess = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite).toAccess()

    override suspend fun request(): PhotoAccess =
        suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite) { continuation.resume(it.toAccess()) }
        }

    override fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any?>(), null)
    }
}

// iOS shows the system dialog only once, so any denial is Blocked.
private fun PHAuthorizationStatus.toAccess(): PhotoAccess =
    when (this) {
        PHAuthorizationStatusAuthorized -> PhotoAccess.Granted
        PHAuthorizationStatusLimited -> PhotoAccess.Limited
        PHAuthorizationStatusNotDetermined -> PhotoAccess.NotDetermined
        else -> PhotoAccess.Blocked
    }
