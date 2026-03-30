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

    val startTime: Long,
    val endTime: Long?,
    val status: String, // ACTIVE, ENDED
    val endType: String?, // MANUAL, AUTO
    val createdAt: Long = System.currentTimeMillis(),
)
