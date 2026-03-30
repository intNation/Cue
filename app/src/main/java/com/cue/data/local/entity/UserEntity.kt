package com.cue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class representing a user in the database.
 * @param id The unique identifier of the user.
 * @param firstName The first name of the user.
 * @param lastName The last name of the user.
 * @param email The email address of the user.
 * @param createdAt The timestamp when the user was created.
 * @constructor Creates a new UserEntity object.
 */
@Entity(tableName = "User")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "first_name")
    val firstName: String,

    @ColumnInfo(name = "last_name")
    val lastName: String,

    @ColumnInfo(name = "email")
    val email: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
