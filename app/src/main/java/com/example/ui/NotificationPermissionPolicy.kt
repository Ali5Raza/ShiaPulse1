package com.example.ui

/** The only setting that may be enabled after a notification-permission result. */
enum class NotificationPermissionTarget {
    ADHAN,
    AAMAL,
    CALENDAR
}

object NotificationPermissionPolicy {
    fun shouldRequestRuntimePermission(sdkInt: Int, permissionGranted: Boolean): Boolean =
        sdkInt >= 33 && !permissionGranted

    /** Denial is deliberately a no-op; a later request can be granted independently. */
    fun handleResult(
        target: NotificationPermissionTarget,
        isGranted: Boolean,
        enableAdhan: () -> Unit,
        enableAamal: () -> Unit,
        enableCalendar: () -> Unit
    ) {
        if (!isGranted) return
        when (target) {
            NotificationPermissionTarget.ADHAN -> enableAdhan()
            NotificationPermissionTarget.AAMAL -> enableAamal()
            NotificationPermissionTarget.CALENDAR -> enableCalendar()
        }
    }
}
