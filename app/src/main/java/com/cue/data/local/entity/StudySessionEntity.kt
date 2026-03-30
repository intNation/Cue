package com.cue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cue.domain.model.EndType
import com.cue.domain.model.SessionStatus
import com.cue.domain.model.StudySession

/**
 * Entity class representing a study session in the database.
 * @param id The unique identifier of the study session.
 * @param startTime The start time of the study session.
 * @param endTime The end time of the study session.
 * @param status The status of the study session (ACTIVE or ENDED).
 * @param endType The end type of the study session (MANUAL or AUTO).
 * @param createdAt The timestamp when the study session was created.
 * @constructor Creates a new StudySessionEntity object.
 */
@Entity(tableName = "StudySession")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val startTime: Long,
    val endTime: Long?,
    val status: String, // ACTIVE, ENDED
    val endType: String?, // MANUAL, AUTO
    val createdAt: Long = System.currentTimeMillis(),
)
