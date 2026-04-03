package com.cue.data.local.mappers

import com.cue.data.local.entity.InsightEntity
import com.cue.domain.model.Insight

fun InsightEntity.toDomain() = Insight(
    id = id,
    userId = userId,
    message = message,
    type = type,
    timestamp = timestamp
)


fun Insight.toEntity() = InsightEntity(
    id = id,
    userId = userId,
    message = message,
    type = type,
    timestamp = timestamp
)