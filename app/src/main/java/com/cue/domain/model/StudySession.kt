package com.cue.domain.model

enum class SessionStatus {
    ACTIVE, ENDED
}

enum class EndType {
    MANUAL, AUTO
}

data class StudySession(
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val endType: EndType? = null
)
