package com.cue.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cue.data.local.entity.StudySessionEntity

@Dao
interface StudySessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Update
    suspend fun updateSession(session: StudySessionEntity)

    @Query("SELECT * FROM StudySession WHERE id = :id")
    suspend fun getSessionById(id: Long): StudySessionEntity?

    @Query("SELECT * FROM StudySession ORDER BY start_time DESC")
    suspend fun getAllSessions(): List<StudySessionEntity>

    @Query("SELECT * FROM StudySession WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveSession(): StudySessionEntity?
}
