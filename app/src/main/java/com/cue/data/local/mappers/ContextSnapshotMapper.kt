package com.cue.data.local.mappers

import com.cue.data.local.entity.ContextSnapshotEntity
import com.cue.domain.model.ContextSnapshot

/**
 * Extension function to convert a ContextSnapshotEntity to a ContextSnapshot domain model.
 */
fun ContextSnapshotEntity.toDomain() = ContextSnapshot(
    id = id,
    sessionId = sessionId,
    studyLocation = studyLocation,
    phoneUsage = phoneUsage,
    connectivity = connectivity,
    weather = weather,
    sleep = sleep,
    confidenceScore = confidenceScore,
    timestamp = timestamp
)

/**
 * Extension function to convert a ContextSnapshot domain model to a ContextSnapshotEntity.
 */
fun ContextSnapshot.toEntity() = ContextSnapshotEntity(
    id = id,
    sessionId = sessionId,
    studyLocation = studyLocation,
    phoneUsage = phoneUsage,
    connectivity = connectivity,
    weather = weather,
    confidenceScore = confidenceScore,
    sleep = sleep,
    timestamp = timestamp
)
