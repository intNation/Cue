package com.cue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Entity class representing a user's anchored study place.
 * @param id The unique identifier of the study place.
 * @param userId The ID of the associated user.
 * @param label A user-defined name for this place.
 * @param category The type of location (e.g., LIBRARY, HOME).
 * @param latitude The coarse latitude anchor.
 * @param longitude The coarse longitude anchor.
 * @param radiusMeters The geofence radius.
 * @param isActive Whether this place is currently tracked.
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

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "location")
    val category: String, // Maps to StudyLocation enum name

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "radius_meters")
    val radiusMeters: Int,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
