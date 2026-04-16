package com.cue.domain.model

enum class StudyLocation(val label: String) {
    LIBRARY("Library"),
    HOME("Home"),
    CAFE("Cafe"),
    OTHER("Other")
}

data class StudyPlace(
    val id: Long,
    val label: String,
    val studyLocation: StudyLocation,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val isActive: Boolean = true

)


enum class SuccessMetric(val label: String, val description: String) {
    COMPLETING_TASK("Completing a task", "Focus on finishing your to-do list items one by one."),
    TIME_DURATION("Studying for 2+ hours", "Prioritize deep-work endurance and time management."),
    NO_DISTRACTION("No distractions", "Achieve a flow state without interruptions or phone use.")
}

data class DaySchedule(
    val dayOfWeek: Int, // 1 (Mon) to 7 (Sun)
    val startTime: String? = null, // HH:mm
    val endTime: String? = null,   // HH:mm
    val isFlexible: Boolean = true
)

data class User(
    val id: Long = 0,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val preferredLocations: List<StudyLocation> = emptyList(),
    val weeklySchedule: List<DaySchedule> = emptyList(),
    val successMetric: SuccessMetric? = null,
    val isOnboardingCompleted: Boolean = false,
    val locationEnabled: Boolean = false,
    val calendarEnabled: Boolean = false,
    val sleepEnabled: Boolean = false,
    val movementEnabled: Boolean = false
)
