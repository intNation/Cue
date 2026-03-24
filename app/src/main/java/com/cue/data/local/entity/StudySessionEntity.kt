package com.cue.data.local.entity
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "StudySession")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "start_time")
    val startTime: Long,

    @ColumnInfo(name = "end_time")
    val endTime: Long?,

    @ColumnInfo(name = "end_type")
    val endType: String? , // Manual or SEMI auto

    @ColumnInfo(name = "created_at")
    val createdAt : Long = System.currentTimeMillis(),
          
)
