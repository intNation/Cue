package com.cue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cue.domain.model.EndType
import com.cue.domain.model.SessionStatus
import com.cue.domain.model.StudySession

@Entity(tableName = "StudySession")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long?,

    @ColumnInfo(name = "status")
    val status: String, // ACTIVE, ENDED

    @ColumnInfo(name = "end_type")
    val endType: String?, // MANUAL, AUTO

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)

fun StudySessionEntity.toDomain() = StudySession(
    id = id,
    startTime = startTime,
    endTime = endTime,
    status = SessionStatus.valueOf(status),
    endType = endType?.let { EndType.valueOf(it) }
)

fun StudySession.toEntity() = StudySessionEntity(
    id = id,
    startTime = startTime,
    endTime = endTime,
    status = status.name,
    endType = endType?.name
)
