package com.cue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Entity class representing a user's preferred study location.
 * @param id The unique identifier of the study location.
 * @param userId The ID of the associated user.
 * @param location The preferred study location (e.g., LIBRARY, HOME, CAFE, OTHER).
 */
@Entity(
    tableName = "StudyLocation",
    foreignKeys = [ForeignKey(
        entity = UserEntity::class,
        parentColumns = ["id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class StudyLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "location")
    val location: String // Stores StudyLocation enum name
)
