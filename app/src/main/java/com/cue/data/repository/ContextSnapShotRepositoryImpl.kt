package com.cue.data.repository

import com.cue.data.local.dao.ContextSnapshotDao
import com.cue.data.local.entity.ContextSnapshotEntity
import com.cue.data.local.mappers.toDomain
import com.cue.domain.model.ContextSnapshot
import com.cue.domain.repository.ContextSnapShotRepository

/**
 * Implementation of the [com.cue.domain.repository.ContextSnapShotRepository] interface.
 * Responsible for managing context snapshots in the database.
 * gets context snapshots from the database and maps them to the domain model.
 * inserts a context snapshot entity to the database using the dao
 * @param dao The context snapshot data access object.
 */
class ContextSnapShotRepositoryImpl(val dao: ContextSnapshotDao) : ContextSnapShotRepository {


    /**
     * Gets a context snapshot by its session ID.
     * @param sessionId The ID of the session.
     * @return The context snapshot, or null if not found.
     */
    override suspend fun getSnapshotBySessionId(sessionId: Long): ContextSnapshot? {
        return dao.getSnapshotBySessionId(sessionId)?.toDomain()
    }

    /**
     * Inserts a context snapshot entity into the database.
     * @param snapshot The context snapshot to insert.
     * @return The ID of the inserted snapshot entity.
     */
    override suspend fun insertSnapshot(snapshot: ContextSnapshot): Long {
        val entity = ContextSnapshotEntity(
            sessionId = snapshot.sessionId,
            phoneUsage = snapshot.phoneUsage,
            connectivity = snapshot.connectivity,
            sleep = snapshot.sleep,
            weather = snapshot.weather,
            confidenceScore = snapshot.confidenceScore,
            timestamp = snapshot.timestamp
        )
        return dao.insertSnapshot(entity)
    }

    override suspend fun getAllSnapshots(): List<ContextSnapshot> {
        return dao.getAllSnapshots().map { it.toDomain() }
    }
}