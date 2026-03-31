package com.cue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.cue.domain.model.ContextSnapshot

/**
 * data class representing a context snapshot in the database.
 * @param id The unique identifier of the context snapshot.
 * @param sessionId The ID of the associated study session.
 * @param phoneUsage The usage of the phone.
 * @param connectivity The connectivity status of the device.
 * @param weather The weather condition.
 * @param confidenceScore The confidence score of the snapshot.
 * @constructor Creates a new ContextSnapshotEntity object.
 */
@Entity(
    tableName = "ContextSnapshot",
   foreignKeys = [ForeignKey(
       entity = StudySessionEntity::class,
       parentColumns = ["id"],
       childColumns = ["session_id"],
       onDelete = ForeignKey.CASCADE
   )]
)
data class ContextSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: Long,

    @ColumnInfo(name = "phone_usage")
    val phoneUsage: String,

    @ColumnInfo(name = "sleep")
    val sleep: Int,  // Mocked

    @ColumnInfo(name = "connectivity")
    val connectivity: String,

    @ColumnInfo(name = "weather")
    val weather: String,

    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float
)

