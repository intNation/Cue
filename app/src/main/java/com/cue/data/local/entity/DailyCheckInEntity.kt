package com.cue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class representing a daily check-in in the database.
 * @param id The unique identifier of the daily check-in.
 * @param timestamp The timestamp of the check-in.
 * @param didStudy A boolean indicating whether the user did study today.
 */
@Entity(tableName = "DailyCheckIn")
data class DailyCheckInEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "did_study")
    val didStudy: Boolean
)

