package com.example.utils

import kotlin.math.*

object QiblaCalculator {
    // Kaaba Coordinates
    private const val KAABA_LAT = 21.4225
    private const val KAABA_LON = 39.8262

    /**
     * Calculates the bearing of the Kaaba relative to North (in degrees, 0 to 360)
     */
    fun calculateQiblaBearing(latitude: Double, longitude: Double): Double {
        val latRad = Math.toRadians(latitude)
        val lonRad = Math.toRadians(longitude)
        val kLatRad = Math.toRadians(KAABA_LAT)
        val kLonRad = Math.toRadians(KAABA_LON)

        val deltaLon = kLonRad - lonRad

        val y = sin(deltaLon)
        val x = cos(latRad) * tan(kLatRad) - sin(latRad) * cos(deltaLon)

        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360.0) % 360.0
        return bearing
    }
}
