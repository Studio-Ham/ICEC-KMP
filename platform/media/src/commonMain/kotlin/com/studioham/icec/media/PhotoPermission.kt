package com.studioham.icec.media

enum class PhotoAccess {
    Granted,
    Limited,
    NotDetermined,

    /** Denied, but the system dialog can be shown again. */
    Denied,

    /** Denied and only the OS settings can grant it. */
    Blocked,
}

interface PhotoPermission {
    fun status(): PhotoAccess

    suspend fun request(): PhotoAccess

    fun openSettings()
}
