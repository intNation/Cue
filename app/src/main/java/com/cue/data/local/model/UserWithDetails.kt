package com.cue.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.cue.data.local.entity.StudyLocationEntity
import com.cue.data.local.entity.UserEntity
import com.cue.data.local.entity.WeeklyScheduleEntity

/**
 * Data class representing a user with their associated preferred locations and weekly schedules.
 */
data class UserWithDetails(
    @Embedded val user: UserEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "user_id"
    )
    val locations: List<StudyLocationEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "user_id"
    )
    val schedules: List<WeeklyScheduleEntity>
)
