package com.cue.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cue.data.local.entity.ContextSnapshotEntity

/**
 * Data access object (DAO) for the ContextSnapshotEntity.
 * Provides methods for inserting and retrieving context snapshots.
 */
@Dao
interface ContextSnapshotDao {

    /**
     * Inserts a context snapshot into the database.
     * @param snapshot The context snapshot entity to insert.
     * @return The ID of the inserted snapshot.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: ContextSnapshotEntity): Long

    /**
     * Retrieves a context snapshot Entity by its session ID.
     * @param sessionId The ID of the session.
     * @return The context snapshot entity, or null if not found.
     */
    @Query("SELECT * FROM ContextSnapshot WHERE session_id = :sessionId")
    suspend fun getSnapshotBySessionId(sessionId: Long): ContextSnapshotEntity?

    @Query("SELECT * FROM ContextSnapshot ORDER BY timestamp DESC")
    suspend fun getAllSnapshots(): List<ContextSnapshotEntity>
}
