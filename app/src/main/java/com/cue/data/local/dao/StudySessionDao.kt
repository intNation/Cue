package com.cue.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cue.data.local.entity.StudySessionEntity

@Dao
interface StudySessionDao {
    @Insert
    suspend fun insertSession(session: StudySessionEntity): Long

    @Query("SELECT * FROM StudySession ORDER BY start_time DESC")
    suspend fun getAllSessions(): List<StudySessionEntity>

    @Query("SELECT * FROM StudySession WHERE end_time IS NULL")
    suspend fun  getActiveStudySessions() : StudySessionEntity?
}