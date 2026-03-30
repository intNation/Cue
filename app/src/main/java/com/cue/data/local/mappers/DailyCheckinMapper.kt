package com.cue.data.local.mappers

import com.cue.data.local.entity.DailyCheckInEntity
import com.cue.domain.model.DailyCheckIn

/**
 * Extension function to convert a DailyCheckInEntity to a DailyCheckIn domain model.
 * @return The converted DailyCheckIn object.
 */
fun DailyCheckInEntity.toDomain() = DailyCheckIn(
    id = id,
    timestamp = timestamp,
    didStudy = didStudy
)

/**
 * Extension function to convert a DailyCheckIn domain model to a DailyCheckInEntity.
 * @return The converted DailyCheckInEntity object.
 */
fun DailyCheckIn.toEntity() = DailyCheckInEntity(
    id = id,
    timestamp = timestamp,
    didStudy = didStudy
)