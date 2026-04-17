package com.cue.domain.model

/**
 * Enum representing the category of a study location.
 */
enum class StudyLocation(val label: String) {
    LIBRARY("Library"),
    HOME("Home"),
    CAFE("Cafe"),
    OTHER("Other")
}

/**
 * Data class representing a specific, anchored study place.
 * @property id The unique identifier of the study place.
 * @property label A user-defined name for this place (e.g., "Main Library 3rd Floor").
 * @property category The type of location.
 * @property latitude The coarse latitude anchor.
 * @property longitude The coarse longitude anchor.
 * @property radiusMeters The geofence radius for matching.
 * @property isActive Whether this anchor is currently used for detection.
 */
data class StudyPlace(
    val id: Long = 0,
    val label: String,
    val category: StudyLocation,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 100,
    val isActive: Boolean = true
)

/**
 * Enum representing user success metrics.
 */
enum class SuccessMetric(val label: String, val description: String) {
    COMPLETING_TASK("Completing a task", "Focus on finishing your to-do list items one by one."),
    TIME_DURATION("Studying for 2+ hours", "Prioritize deep-work endurance and time management."),
    NO_DISTRACTION("No distractions", "Achieve a flow state without interruptions or phone use.")
}

/**
 * Data class representing a scheduled study window.
 */
data class DaySchedule(
    val dayOfWeek: Int, // 1 (Mon) to 7 (Sun)
    val startTime: String? = null, // HH:mm
    val endTime: String? = null,   // HH:mm
    val isFlexible: Boolean = true
)

/**
 * Data class representing the User domain model.
 */
data class User(
    val id: Long = 0,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val studyPlaces: List<StudyPlace> = emptyList(),
    val weeklySchedule: List<DaySchedule> = emptyList(),
    val successMetric: SuccessMetric? = null,
    val isOnboardingCompleted: Boolean = false,
    val locationEnabled: Boolean = false,
    val calendarEnabled: Boolean = false,
    val sleepEnabled: Boolean = false,
    val movementEnabled: Boolean = false,
    val phoneUsageEnabled: Boolean = false
)
