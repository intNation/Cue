package com.cue.data.local.mappers

import com.cue.data.local.entity.ContextSnapshotEntity
import com.cue.domain.model.ContextSnapshot

/**
 * Extension function to convert a ContextSnapshotEntity to a ContextSnapshot domain model.
 * @return The converted ContextSnapshot object.
 */
fun ContextSnapshotEntity.toDomain() = ContextSnapshot(
    id = id,
    sessionId = sessionId,
    phoneUsage = phoneUsage,
    connectivity = connectivity,
    weather = weather,
    sleep = sleep,
    confidenceScore = confidenceScore,
)

/**
 * Extension function to convert a ContextSnapshot domain model to a ContextSnapshotEntity.
 * @return The converted ContextSnapshotEntity object.
 */
fun ContextSnapshot.toDomain() = ContextSnapshotEntity(
    id = id,
    sessionId = sessionId,
    phoneUsage = phoneUsage,
    connectivity = connectivity,
    weather = weather,
    confidenceScore = confidenceScore,
    sleep = sleep
)