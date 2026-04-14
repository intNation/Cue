package com.cue.context.provider

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.cue.domain.model.StudyLocation

/**
 * Provider for collecting approximate location data.
 */
class LocationProvider(private val context: Context) {

    /**
     * Gets the last known approximate location.
     * @return Pair of Latitude and Longitude, or null if unavailable.
     */
    @SuppressLint("MissingPermission")
    fun getLastLocation(): Pair<Double, Double>? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null

        for (provider in providers) {
            val l = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }

        return bestLocation?.let { it.latitude to it.longitude }
    }
}
