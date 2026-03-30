package com.cue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.cue.domain.model.ContextSnapshot

@Entity(
    tableName = "ContextSnapshot",
    foreignKeys = [
        ForeignKey(
            entity = StudySessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ContextSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "phone_usage")
    val phoneUsage: String,

    @ColumnInfo(name = "connectivity")
    val connectivity: String,

    @ColumnInfo(name = "weather")
    val weather: String,

    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float
)

