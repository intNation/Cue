package com.cue.domain.model

/**
 * Enum representing the status of a study session.
 */
enum class SessionStatus {
    ACTIVE, ENDED
}

/**
 * Enum representing the end type of a study session.
 */
enum class EndType {
    MANUAL, AUTO
}

/**
 * Data class representing a study session.
 * @param id The unique identifier of the study session.
 * @param startTime The start time of the study session.
 * @param endTime The end time of the study session.
 * @param status The status of the study session.
 * @param endType The end type of the study session.
 * @constructor Creates a new StudySession object.
 */
data class StudySession(
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val endType: EndType? = null
)
