package com.cue.data.local.entity

import com.cue.domain.model.ContextSnapshot

fun ContextSnapshotEntity.toDomain() = ContextSnapshot(
    id = id,
    sessionId = sessionId,
    phoneUsage = phoneUsage,
    connectivity = connectivity,
    weather = weather,
    confidenceScore = confidenceScore,
)
fun ContextSnapshot.toDomain() = ContextSnapshotEntity(
    id = id,
    sessionId = sessionId,
    phoneUsage = phoneUsage,
    connectivity = connectivity,
    weather = weather,
    confidenceScore = confidenceScore,
)