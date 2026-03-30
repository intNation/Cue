package com.cue.data.local.entity

import com.cue.domain.model.EndType
import com.cue.domain.model.SessionStatus
import com.cue.domain.model.StudySession

/**
 * Extension function to convert a StudySessionEntity to a StudySession domain model.
 * @return The converted StudySession object.
 * @receiver The StudySessionEntity to be converted.
 */
fun StudySessionEntity.toDomain() = StudySession(
    id = id,
    startTime = startTime,
    endTime = endTime,
    status = SessionStatus.valueOf(status),
    endType = endType?.let { EndType.valueOf(it) }
)

/**
 * Extension function to convert a StudySession domain model to a StudySessionEntity.
 * @return The converted StudySessionEntity object.
 * @receiver The StudySession to be converted.
 */
fun StudySession.toEntity() = StudySessionEntity(
    id = id,
    startTime = startTime,
    endTime = endTime,
    status = status.name,
    endType = endType?.name
)

