package com.cue.data.local.entity

import com.cue.domain.model.EndType
import com.cue.domain.model.SessionStatus
import com.cue.domain.model.StudySession

fun StudySessionEntity.toDomain() = StudySession(
    id = id,
    startTime = startTime,
    endTime = endTime,
    status = SessionStatus.valueOf(status),
    endType = endType?.let { EndType.valueOf(it) }
)

fun StudySession.toEntity() = StudySessionEntity(
    id = id,
    startTime = startTime,
    endTime = endTime,
    status = status.name,
    endType = endType?.name
)

