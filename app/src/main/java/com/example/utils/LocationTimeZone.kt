package com.example.utils

import java.util.SimpleTimeZone
import java.util.TimeZone

enum class LocationTimeZoneSource {
    IANA_ID,
    FIXED_OFFSET,
    DEVICE_NO_SELECTION,
    DEVICE_INVALID_SELECTION
}

data class ResolvedLocationTimeZone(
    val timeZone: TimeZone,
    val source: LocationTimeZoneSource
)

/** Resolves persisted location metadata without silently accepting an invalid time-zone ID. */
object LocationTimeZone {
    private val availableIds: Set<String> by lazy { TimeZone.getAvailableIDs().toHashSet() }

    /**
     * Valid IANA IDs win, then valid legacy fixed offsets. With no selected location the
     * device zone is the legacy default. Invalid selected metadata also deliberately uses
     * the device zone for every reminder consumer; it is never silently converted to GMT.
     */
    fun resolveDetailed(
        timeZoneId: String?,
        fallbackOffsetHours: Double?,
        hasSelectedLocation: Boolean = true,
        deviceTimeZone: TimeZone = TimeZone.getDefault()
    ): ResolvedLocationTimeZone {
        val id = timeZoneId?.trim().orEmpty()
        if (id.isNotEmpty() && isValidId(id)) {
            return ResolvedLocationTimeZone(TimeZone.getTimeZone(id), LocationTimeZoneSource.IANA_ID)
        }
        if (isValidOffset(fallbackOffsetHours)) {
            return ResolvedLocationTimeZone(
                fixedOffset(requireNotNull(fallbackOffsetHours)),
                LocationTimeZoneSource.FIXED_OFFSET
            )
        }
        val source = if (hasSelectedLocation) {
            LocationTimeZoneSource.DEVICE_INVALID_SELECTION
        } else {
            LocationTimeZoneSource.DEVICE_NO_SELECTION
        }
        return ResolvedLocationTimeZone(deviceTimeZone, source)
    }

    fun resolve(timeZoneId: String?, fallbackOffsetHours: Double): TimeZone =
        resolveDetailed(timeZoneId, fallbackOffsetHours).timeZone

    fun isValidId(timeZoneId: String): Boolean =
        timeZoneId in availableIds || timeZoneId in setOf("GMT", "UTC", "UT")

    fun isValidOffset(offsetHours: Double?): Boolean =
        offsetHours != null && offsetHours.isFinite() && Math.abs(offsetHours) <= 18.0

    fun fixedOffset(offsetHours: Double): TimeZone {
        val safeHours = if (isValidOffset(offsetHours)) offsetHours else 0.0
        val offsetMillis = (safeHours * 60.0 * 60.0 * 1000.0).toInt()
        val totalMinutes = offsetMillis / 60_000
        val sign = if (totalMinutes < 0) "-" else "+"
        val absoluteMinutes = Math.abs(totalMinutes)
        val id = String.format(
            java.util.Locale.US,
            "GMT%s%02d:%02d",
            sign,
            absoluteMinutes / 60,
            absoluteMinutes % 60
        )
        return SimpleTimeZone(offsetMillis, id)
    }
}

enum class DeviceLocationAccuracy {
    HIGH_ACCURACY,
    BALANCED_APPROXIMATE
}

/** Fine permission is preferred, but Android's approximate/coarse grant remains usable. */
fun resolveDeviceLocationAccuracy(
    fineGranted: Boolean,
    coarseGranted: Boolean
): DeviceLocationAccuracy? = when {
    fineGranted -> DeviceLocationAccuracy.HIGH_ACCURACY
    coarseGranted -> DeviceLocationAccuracy.BALANCED_APPROXIMATE
    else -> null
}
