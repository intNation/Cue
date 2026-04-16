package com.cue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Entity class representing a user's weekly study schedule.
 * @param id The unique identifier of the schedule.
 * @param userId The ID of the associated user.
 * @param dayOfWeek The day of the week (1 = Monday, ..., 7 = Sunday).
 * @param startTime The start time of the study session (Format: "HH:mm").
 * @param endTime The end time of the study session (Format: "HH:mm").
 * @param isFlexible A boolean indicating whether the schedule is flexible.
 */
@Entity(
    tableName = "WeeklySchedule",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class WeeklyScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "day_of_week")
    val dayOfWeek: Int, // 1 (Mon) to 7 (Sun)

    @ColumnInfo(name = "start_time")
    val startTime: String?, // "HH:mm"

    @ColumnInfo(name = "end_time")
    val endTime: String?,   // "HH:mm"

    @ColumnInfo(name = "is_flexible")
    val isFlexible: Boolean = true
)
