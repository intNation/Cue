package com.cue.data.local.mappers

import com.cue.data.local.entity.InsightEntity
import com.cue.domain.model.Insight
import com.cue.domain.model.InsightType

fun InsightEntity.toDomain() = Insight(
    id = id,
    userId = userId,
    message = message,
    type = InsightType.valueOf(type),
    timestamp = timestamp,
    confidenceScore = confidenceScore
)


fun Insight.toEntity() = InsightEntity(
    id = id,
    userId = userId,
    message = message,
    type = type.name,
    timestamp = timestamp,
    confidenceScore = confidenceScore
)