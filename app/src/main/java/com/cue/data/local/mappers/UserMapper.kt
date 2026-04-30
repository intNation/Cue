package com.cue.data.local.mappers

import com.cue.data.local.entity.StudyLocationEntity
import com.cue.data.local.entity.UserEntity
import com.cue.data.local.entity.WeeklyScheduleEntity
import com.cue.data.local.model.UserWithDetails
import com.cue.domain.model.DaySchedule
import com.cue.domain.model.StudyLocation
import com.cue.domain.model.StudyPlace
import com.cue.domain.model.SuccessMetric
import com.cue.domain.model.User

/**
 * Extension function to convert a UserWithDetails to a User domain model.
 */
fun UserWithDetails.toDomain() = User(
    id = user.id,
    firstName = user.firstName,
    lastName = user.lastName,
    email = user.email,
    studyPlaces = locations.map { it.toDomain() },
    weeklySchedule = schedules.map { it.toDomain() },
    successMetric = user.successMetric?.let { SuccessMetric.valueOf(it) },
    isOnboardingCompleted = user.isOnboardingCompleted,
    locationEnabled = user.locationEnabled,
    calendarEnabled = user.calendarEnabled,
    sleepEnabled = user.sleepEnabled,
    movementEnabled = user.movementEnabled,
    phoneUsageEnabled = user.phoneUsageEnabled
)

/**
 * Extension function to convert a StudyLocationEntity to a StudyPlace domain model.
 */
fun StudyLocationEntity.toDomain() = StudyPlace(
    id = id,
    label = label,
    category = StudyLocation.valueOf(category),
    latitude = latitude,
    longitude = longitude,
    radiusMeters = radiusMeters,
    isActive = isActive
)

/**
 * Extension function to convert a WeeklyScheduleEntity to a DaySchedule domain model.
 */
fun WeeklyScheduleEntity.toDomain() = DaySchedule(
    dayOfWeek = dayOfWeek,
    startTime = startTime,
    endTime = endTime,
    isFlexible = isFlexible
)

/**
 * Extension function to convert a User domain model to its respective entities for saving.
 */
fun User.toEntity() = UserEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    successMetric = successMetric?.name,
    isOnboardingCompleted = isOnboardingCompleted,
    locationEnabled = locationEnabled,
    calendarEnabled = calendarEnabled,
    sleepEnabled = sleepEnabled,
    movementEnabled = movementEnabled,
    phoneUsageEnabled = phoneUsageEnabled,
    createdAt = System.currentTimeMillis()
)

/**
 * Extension function to convert a list of StudyPlaces to entities.
 */
fun User.toStudyPlaceEntities() = studyPlaces.map {
    StudyLocationEntity(
        id = it.id,
        userId = id,
        label = it.label,
        category = it.category.name,
        latitude = it.latitude,
        longitude = it.longitude,
        radiusMeters = it.radiusMeters,
        isActive = it.isActive
    )
}

/**
 * Extension function to convert a list of DaySchedules to entities.
 */
fun User.toScheduleEntities() = weeklySchedule.map {
    WeeklyScheduleEntity(
        userId = id,
        dayOfWeek = it.dayOfWeek,
        startTime = it.startTime,
        endTime = it.endTime,
        isFlexible = it.isFlexible
    )
}
