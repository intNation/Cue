package com.cue.data.local.entity

import com.cue.domain.model.DailyCheckIn

fun DailyCheckInEntity.toDomain() = DailyCheckIn(
    id = id,
    timestamp = timestamp,
    didStudy = didStudy
)

fun DailyCheckIn.toEntity() = DailyCheckInEntity(
    id = id,
    timestamp = timestamp,
    didStudy = didStudy
)