package com.cue.domain.model

/**
 * Data class representing a daily check-in.
 * @param id The unique identifier of the check-in.
 * @param timestamp The timestamp of the check-in.
 * @param didStudy A boolean indicating whether the user did study today.
 * @constructor Creates a new DailyCheckIn object.
 */
data class DailyCheckIn(
    val id: Long = 0,
    val timestamp: Long,
    val didStudy: Boolean
)
