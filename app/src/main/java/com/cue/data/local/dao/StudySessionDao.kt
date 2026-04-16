package com.cue.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cue.data.local.entity.StudySessionEntity

/**
 * Data access object (DAO) for the StudySessionEntity.
 * Provides methods for inserting, updating, and retrieving study sessions.
 * @constructor Creates a new StudySessionDao object.
 */
@Dao
interface StudySessionDao {
    /**
     * Inserts a study session entity into the database.
     * if the session exists, it will be replaced
     * @param session The study session entity to insert.
     * @return The ID of the inserted session entity.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    /**
     * Updates a study session entity in the database.
     * @param session The study session entity to update.
     */
    @Update
    suspend fun updateSession(session: StudySessionEntity)

    /**
     * Retrieves a study session entity by its ID.
     * @param id The ID of the study session.
     * @return The study session entity, or null if not found.
     */

    @Query("SELECT * FROM StudySession WHERE id = :id")
    suspend fun getSessionById(id: Long): StudySessionEntity?

    /**
     * Retrieves all study session entities from the database.
     * @return A list of study session entities.
     */
    @Query("SELECT * FROM StudySession ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<StudySessionEntity>

    /**
     * Retrieves the active study session entity from the database.
     * @return The active study session entity, or null if no active session is found.
     */
    @Query("SELECT * FROM StudySession WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveSession(): StudySessionEntity?
}
