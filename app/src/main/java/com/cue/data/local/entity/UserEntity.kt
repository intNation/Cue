package com.cue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class representing a user in the database.
 * @property id The unique identifier of the user.
 * @property firstName The first name of the user.
 * @property lastName The last name of the user.
 * @property email The email address of the user.
 * @property successMetric The user's success metric.
 *
 *
 */
@Entity(tableName = "User")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "first_name")
    val firstName: String = "",

    @ColumnInfo(name = "last_name")
    val lastName: String = "",

    @ColumnInfo(name = "email")
    val email: String = "",

    @ColumnInfo(name = "success_metric")
    val successMetric: String? = null, // Enum name

    @ColumnInfo(name = "is_onboarding_completed")
    val isOnboardingCompleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
