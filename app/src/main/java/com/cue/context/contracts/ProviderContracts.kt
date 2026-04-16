package com.cue.context.contracts

import com.cue.domain.model.StudyPlace

/**
 * Enum representing the different reasons why a provider is unavailable.
 * @property PERMISSION_DENIED Permission was denied by the user.
 * @property SYSTEM_SETTING_DISABLED The system setting is disabled.
 * @property DATA_NOT_AVAILABLE No data is available.
 * @property TIMEOUT The operation timed out.
 * @property TEMPORARY_ERROR A temporary error occurred.
 */
enum class UnavailableReason {
    PERMISSION_DENIED,
    SYSTEM_SETTING_DISABLED,
    DATA_NOT_AVAILABLE,
    TIMEOUT,
    TEMPORARY_ERROR
}

/**
 * Enum representing the different levels of phone usage
 */
enum class PhoneUsageLevelSignal{
    LOW,MEDIUM,HIGH,UNKNOWN
}

/**
 * Enum representing the different types of connectivity
 */
enum class ConnectivitySignal{
    WIFI,CELLULAR,NONE,UNKNOWN
}

/**
 * Enum representing the different types of study location
 * @property AT_USUAL_LOCATION The user is at a usual location
 * @property AWAY_FROM_USUAL_LOCATION The user is away from a usual location
 * @property UNKNOWN The user's location is unknown
 */
enum class StudyLocationSignal{
    AT_USUAL_LOCATION, AWAY_FROM_USUAL_LOCATION, UNKNOWN
}

/**
 * Enum representing the different types of whether
 */
enum class WhetherSignal{
    SUNNY,RAINY,CLOUDY,UNKNOWN
}

/**
 * Data class representing a coarse location with latitude and longitude
 * @param latitude The latitude of the location
 * @param longitude The longitude of the location
 */
data class CoarseLocation(val latitude: Double, val longitude: Double)

/**
 * Sealed class representing the result of a provider operation
 * @param T The type of data returned by the provider
 * @property Available The operation was successful and data is available
 * @property Unavailable The operation failed and no data is available
 */
sealed class ProviderResult<out T> {
    data class Available<out T>(val data: T) : ProviderResult<T>()
    data class Unavailable(val reason: UnavailableReason) : ProviderResult<Nothing>()
}

/**
 * Interface for a phone usage level provider
 */
interface PhoneUsageLevelProvider {
    /**
     * Gets the current phone usage level
     * @return The current phone usage level
     */
    suspend fun getPhoneUsageLevel(): ProviderResult<PhoneUsageLevelSignal>
}

/**
 * Interface for a connectivity provider
 */
interface ConnectivityProvider {
    /**
     * Gets the current connectivity signal
     * @return The current connectivity signal
     */
     fun getConnectivitySignal(): ProviderResult<ConnectivitySignal>
}

/**
 * Interface for a study location provider
 *
 */
interface StudyLocationProvider {
    /**
     * Gets the current study location signal
     * @return The current study location signal
     */
     suspend fun  getStudyLocationSignal(studyPlaces: List<StudyPlace>): ProviderResult<StudyLocationSignal>

    /**
     * Gets the current coarse location to be used for whether
     * @return The current coarse location
     */
    suspend fun  getWhetherLocation(): ProviderResult<CoarseLocation>
}


/**
 * Interface for a whether provider
 */
interface WhetherProvider {
    /**
     * Gets the current whether signal
     * @return The current whether signal
     */
    suspend fun getWhetherSignal(location: CoarseLocation): ProviderResult<WhetherSignal>
}




