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
import com.cue.domain.model.StudyPlace

/**
 * Provider for collecting privacy-safe location-derived signals.
 */
class LocationProvider(private val context: Context) : StudyLocationProvider {

    /**
     * Returns a study-location classification based on anchored places.
     */
    override suspend fun getStudyLocationSignal(
        studyPlaces: List<StudyPlace>
    ): ProviderResult<StudyLocationSignal> {
        // 1. Check for coarse location permission
        if (!hasCoarseLocationPermission()) {
            return ProviderResult.Unavailable(UnavailableReason.PERMISSION_DENIED)
        }

        // 2. Check if system location services are enabled
        if (!isLocationEnabled()) {
            return ProviderResult.Unavailable(UnavailableReason.SYSTEM_SETTING_DISABLED)
        }

        // 3. Check for saved study anchors
        if (studyPlaces.isEmpty()) {
            return ProviderResult.Unavailable(UnavailableReason.DATA_NOT_AVAILABLE)
        }

        // 4. Get the last known location
        val location = getBestLastKnownLocation()
            ?: return ProviderResult.Unavailable(UnavailableReason.DATA_NOT_AVAILABLE)

        // 5. Verify accuracy
        if (!isReasonablyAccurate(location)) {
            return ProviderResult.Unavailable(UnavailableReason.DATA_NOT_AVAILABLE)
        }

        // 6. Geofence matching
        val isAtUsualLocation = studyPlaces.any { place ->
            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                place.latitude, place.longitude,
                results
            )
            results[0] <= place.radiusMeters
        }

        return if (isAtUsualLocation) {
            ProviderResult.Available(StudyLocationSignal.AT_USUAL_LOCATION)
        } else {
            ProviderResult.Available(StudyLocationSignal.AWAY_FROM_USUAL_LOCATION)
        }
    }

    /**
     * Returns only quantized coordinates for weather lookup.
     */
    override suspend fun getWhetherLocation(): ProviderResult<CoarseLocation> {
        // 1. Check for coarse location permission
        if (!hasCoarseLocationPermission()) {
            return ProviderResult.Unavailable(UnavailableReason.PERMISSION_DENIED)
        }

        // 2. Check if system location services are enabled
        if (!isLocationEnabled()) {
            return ProviderResult.Unavailable(UnavailableReason.SYSTEM_SETTING_DISABLED)
        }

        // 3. Get the last known location
        val location = getBestLastKnownLocation()
            ?: return ProviderResult.Unavailable(UnavailableReason.DATA_NOT_AVAILABLE)

        // 4. Verify accuracy
        if (!isReasonablyAccurate(location)) {
            return ProviderResult.Unavailable(UnavailableReason.DATA_NOT_AVAILABLE)
        }

        // 5. Return quantized coordinates
        return ProviderResult.Available(
            CoarseLocation(
                latitude = quantizeCoordinate(location.latitude),
                longitude = quantizeCoordinate(location.longitude)
            )
        )
    }

    /**
     * Checks if the app has coarse location permission.
     */
    private fun hasCoarseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if system location services (GPS or Network) are enabled.
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    private fun getBestLastKnownLocation(): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val providers =  locationManager.getProviders(true)
        var bestLocation: Location? = null

        for (provider in providers) {
            val l = try {
                locationManager.getLastKnownLocation(provider)
            } catch (e: SecurityException) {
                null
            } ?: continue

            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }

        return bestLocation
    }

    private fun isReasonablyAccurate(location: Location): Boolean {
        return !location.hasAccuracy() || location.accuracy <= MAX_COARSE_ACCURACY_METERS
    }

    private fun quantizeCoordinate(value: Double): Double {
        return kotlin.math.round(value * COARSE_GRID_SCALE) / COARSE_GRID_SCALE
    }

    companion object {
        private const val MAX_COARSE_ACCURACY_METERS = 3_000f
        private const val COARSE_GRID_SCALE = 10.0
    }
}
