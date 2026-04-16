package com.cue.context.provider

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.cue.context.contracts.CoarseLocation
import com.cue.context.contracts.ProviderResult
import com.cue.context.contracts.StudyLocationProvider
import com.cue.context.contracts.StudyLocationSignal
import com.cue.context.contracts.UnavailableReason
import com.cue.domain.model.StudyLocation

/**
 * Provider for collecting privacy-safe location-derived signals.
 */
class LocationProvider(private val context: Context) : StudyLocationProvider {

    /**
     * Returns a conservative study-location classification.
     *
     * The current domain model only stores place categories such as HOME and LIBRARY,
     * not user-approved coordinate anchors. Until that data exists, this provider avoids
     * pretending it can determine whether the user is at a usual study location.
     */
    override suspend fun getStudyLocationSignal(
        savedLocations: List<StudyLocation>
    ): ProviderResult<StudyLocationSignal> {
        // Check for coarse location permission
        // If missing, return a Permission denied reason
        if (!hasCoarseLocationPermission()) {
            return ProviderResult.Unavailable(UnavailableReason.PERMISSION_DENIED)
        }

        //  If there are no saved study locations, we can't determine if the user is at a study location or not, so return data not available
        if (savedLocations.isEmpty()) {
            return ProviderResult.Unavailable(UnavailableReason.DATA_NOT_AVAILABLE)
        }

        // Get the last known location
        // If missing, return data not available
        // If not accurate enough, return data not available
        val location = getBestLastKnownLocation()
            ?: return ProviderResult.Unavailable(UnavailableReason.DATA_NOT_AVAILABLE)

        // Check if the location is accurate enough
        // If not, return data not available
        return if (isReasonablyAccurate(location)) {
            ProviderResult.Available(StudyLocationSignal.UNKNOWN)
        } else {
            ProviderResult.Unavailable(UnavailableReason.DATA_NOT_AVAILABLE)
        }
    }

    /**
     * Returns only quantized coordinates for weather lookup.
     */
    override suspend fun getWhetherLocation(): ProviderResult<CoarseLocation> {

        // Check for coarse location permission
        if (!hasCoarseLocationPermission()) {
            return ProviderResult.Unavailable(UnavailableReason.PERMISSION_DENIED)
        }

        // Get the last known location
        // If missing, return data not available
        val location = getBestLastKnownLocation()
            ?: return ProviderResult.Unavailable(UnavailableReason.DATA_NOT_AVAILABLE)

        // Check if the location is accurate enough
        // If not, return data not available
        if (!isReasonablyAccurate(location)) {
            return ProviderResult.Unavailable(UnavailableReason.DATA_NOT_AVAILABLE)
        }

        // Return the location coordinates
        return ProviderResult.Available(
            CoarseLocation(
                latitude = quantizeCoordinate(location.latitude),
                longitude = quantizeCoordinate(location.longitude)
            )
        )
    }

    /**
     * Checks if the app has coarse location permission.
     * @return True if the permission is granted, false otherwise.
     */
    private fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getBestLastKnownLocation(): Location? {
        // Get the system LocationManager
        // If missing, return null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        // Get the list of available providers
        // If empty, return null
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null

        // Iterate over the providers to find the best location
        // If no location is found, return null
        for (provider in providers) {
            val l = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }

        return bestLocation
    }

    /**
     * Checks if a location is reasonably accurate.
     * @param location The location to check.
     * @return True if the location is reasonably accurate, false otherwise.
     */
    private fun isReasonablyAccurate(location: Location): Boolean {
        return !location.hasAccuracy() || location.accuracy <= MAX_COARSE_ACCURACY_METERS
    }

    /**
     * Quantizes a coordinate to a coarse grid.
     * @param value The coordinate to quantize.
     * @return The quantized coordinate.
     */
    private fun quantizeCoordinate(value: Double): Double {
        // Round the coordinate to the nearest 0.1 degrees
        // and then scale it back to the original range

        return kotlin.math.round(value * COARSE_GRID_SCALE) / COARSE_GRID_SCALE
    }

    /**
     * Companion object containing constants used in the provider.
     * @property MAX_COARSE_ACCURACY_METERS The maximum accuracy for a coarse location in meters.
     * @property COARSE_GRID_SCALE The scale factor for quantizing coordinates.
     */
    companion object {
        private const val MAX_COARSE_ACCURACY_METERS = 3_000f
        private const val COARSE_GRID_SCALE = 10.0
    }
}
